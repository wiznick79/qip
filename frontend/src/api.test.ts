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
    expect(init?.headers).toBeUndefined();
  });
});

function jsonResponse(value: unknown) {
  return new Response(JSON.stringify(value), { status: 200, headers: { "Content-Type": "application/json" } });
}
