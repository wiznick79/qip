import { api, ApiError } from "./api";

describe("API client", () => {
  afterEach(() => vi.restoreAllMocks());

  it("surfaces RFC 9457 field validation details", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({
      title: "Request validation failed",
      detail: "One or more request fields are invalid.",
      status: 400,
      errors: { name: "must not be blank" },
    }), { status: 400, headers: { "Content-Type": "application/problem+json" } }));

    const error = await api.createAsset({ name: "", type: "MACHINE", externalReference: null })
      .catch((cause: unknown) => cause);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ message: "must not be blank", status: 400 });
  });

  it("leaves multipart content type to the browser", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ id: "document-1" }));
    const file = new File(["synthetic"], "guide.txt", { type: "text/plain" });

    await api.uploadDocument("Synthetic guide", file);

    const init = fetchMock.mock.calls[0][1];
    expect(init?.body).toBeInstanceOf(FormData);
    expect(new Headers(init?.headers).has("Content-Type")).toBe(false);
  });

  it("uses the server CSRF token for login and state-changing requests", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockImplementation(async (input) => {
      if (String(input) === "/api/session") {
        return jsonResponse({
          authenticated: false,
          username: null,
          roles: [],
          csrfHeaderName: "X-CSRF-TOKEN",
          csrfToken: "csrf-14",
        });
      }
      return new Response(null, { status: 204 });
    });

    await api.getSession();
    await api.login("qip-investigator", "local-password");

    const login = fetchMock.mock.calls.find(([path]) => String(path) === "/api/session/login");
    const headers = new Headers(login?.[1]?.headers);
    expect(headers.get("X-CSRF-TOKEN")).toBe("csrf-14");
    expect(headers.get("Content-Type")).toBe("application/x-www-form-urlencoded");
  });
});

function jsonResponse(value: unknown) {
  return new Response(JSON.stringify(value), { status: 200, headers: { "Content-Type": "application/json" } });
}
