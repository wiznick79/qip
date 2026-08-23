Add-Type -AssemblyName System.Net.Http

function Assert-QipSuccessResponse {
    param(
        [System.Net.Http.HttpResponseMessage]$Response,
        [string]$Operation
    )

    $responseBody = $Response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    if (-not $Response.IsSuccessStatusCode) {
        throw "$Operation failed with HTTP $([int]$Response.StatusCode): $responseBody"
    }
    return $responseBody
}

function Get-QipSession {
    param([pscustomobject]$Context)

    $response = $Context.Client.GetAsync("$($Context.BaseUrl)/api/session").GetAwaiter().GetResult()
    try {
        $responseBody = Assert-QipSuccessResponse -Response $response -Operation "Reading the QIP session"
        return $responseBody | ConvertFrom-Json
    }
    finally {
        $response.Dispose()
    }
}

function New-QipAuthenticatedClient {
    param(
        [string]$BaseUrl,
        [string]$Username,
        [string]$Password
    )

    $handler = [System.Net.Http.HttpClientHandler]::new()
    $handler.UseCookies = $true
    $handler.CookieContainer = [System.Net.CookieContainer]::new()
    $client = [System.Net.Http.HttpClient]::new($handler, $true)
    $context = [pscustomobject]@{
        BaseUrl = $BaseUrl.TrimEnd("/")
        Client = $client
        CsrfHeaderName = $null
        CsrfToken = $null
    }

    try {
        $anonymousSession = Get-QipSession -Context $context
        $context.CsrfHeaderName = $anonymousSession.csrfHeaderName
        $context.CsrfToken = $anonymousSession.csrfToken

        $pairs = [System.Collections.Generic.List[System.Collections.Generic.KeyValuePair[string,string]]]::new()
        $pairs.Add([System.Collections.Generic.KeyValuePair[string,string]]::new("username", $Username))
        $pairs.Add([System.Collections.Generic.KeyValuePair[string,string]]::new("password", $Password))
        $request = [System.Net.Http.HttpRequestMessage]::new(
            [System.Net.Http.HttpMethod]::Post,
            "$($context.BaseUrl)/api/session/login"
        )
        $request.Headers.TryAddWithoutValidation($context.CsrfHeaderName, $context.CsrfToken) | Out-Null
        $request.Content = [System.Net.Http.FormUrlEncodedContent]::new($pairs)
        $response = $client.SendAsync($request).GetAwaiter().GetResult()
        try {
            Assert-QipSuccessResponse -Response $response -Operation "Signing in to QIP as $Username" | Out-Null
        }
        finally {
            $response.Dispose()
            $request.Dispose()
        }

        # Spring Security rotates the CSRF token after authentication.
        $authenticatedSession = Get-QipSession -Context $context
        if (-not $authenticatedSession.authenticated) {
            throw "QIP did not establish an authenticated session for $Username."
        }
        $context.CsrfHeaderName = $authenticatedSession.csrfHeaderName
        $context.CsrfToken = $authenticatedSession.csrfToken
        return $context
    }
    catch {
        $client.Dispose()
        throw
    }
}

function Invoke-QipJson {
    param(
        [pscustomobject]$Context,
        [string]$Method,
        [string]$Path,
        [object]$Body
    )

    $request = [System.Net.Http.HttpRequestMessage]::new(
        [System.Net.Http.HttpMethod]::new($Method),
        "$($Context.BaseUrl)$Path"
    )
    if ($Method -notin @("GET", "HEAD", "OPTIONS")) {
        $request.Headers.TryAddWithoutValidation($Context.CsrfHeaderName, $Context.CsrfToken) | Out-Null
    }
    if ($null -ne $Body) {
        $json = $Body | ConvertTo-Json -Depth 10 -Compress
        $request.Content = [System.Net.Http.StringContent]::new(
            $json,
            [System.Text.Encoding]::UTF8,
            "application/json"
        )
    }

    $response = $Context.Client.SendAsync($request).GetAwaiter().GetResult()
    try {
        $responseBody = Assert-QipSuccessResponse -Response $response -Operation "$Method $Path"
        if ([string]::IsNullOrWhiteSpace($responseBody)) {
            return $null
        }
        return $responseBody | ConvertFrom-Json
    }
    finally {
        $response.Dispose()
        $request.Dispose()
    }
}

function Send-QipDocument {
    param(
        [pscustomobject]$Context,
        [string]$Title,
        [string]$DocumentPath
    )

    $form = [System.Net.Http.MultipartFormDataContent]::new()
    $request = $null
    $response = $null
    try {
        $titleContent = [System.Net.Http.StringContent]::new($Title, [System.Text.Encoding]::UTF8)
        $fileContent = [System.Net.Http.ByteArrayContent]::new([System.IO.File]::ReadAllBytes($DocumentPath))
        $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::new("application/pdf")
        $form.Add($titleContent, "title")
        $form.Add($fileContent, "file", [System.IO.Path]::GetFileName($DocumentPath))

        $request = [System.Net.Http.HttpRequestMessage]::new(
            [System.Net.Http.HttpMethod]::Post,
            "$($Context.BaseUrl)/api/documents"
        )
        $request.Headers.TryAddWithoutValidation($Context.CsrfHeaderName, $Context.CsrfToken) | Out-Null
        $request.Content = $form
        $response = $Context.Client.SendAsync($request).GetAwaiter().GetResult()
        $responseBody = Assert-QipSuccessResponse -Response $response -Operation "Uploading $Title"
        return $responseBody | ConvertFrom-Json
    }
    finally {
        if ($null -ne $response) {
            $response.Dispose()
        }
        if ($null -ne $request) {
            $request.Dispose()
        }
        else {
            $form.Dispose()
        }
    }
}
