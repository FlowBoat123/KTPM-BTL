"""Export the running application's OpenAPI contract as a reviewable JSON file."""
import argparse
import json
from pathlib import Path
from urllib.request import urlopen

parser = argparse.ArgumentParser()
parser.add_argument("--base-url", default="http://localhost:8080")
parser.add_argument("--input", type=Path, help="Normalize a previously exported runtime snapshot")
parser.add_argument("--output", type=Path, default=Path("docs/openapi.json"))
args = parser.parse_args()
if args.input:
    spec = json.loads(args.input.read_text(encoding="utf-8"))
else:
    with urlopen(args.base_url.rstrip("/") + "/v3/api-docs", timeout=30) as response:
        spec = json.load(response)
assert spec["openapi"].startswith("3.")
spec["servers"] = [{"url": "http://localhost:8080", "description": "Local / Docker"}]
# Springdoc otherwise infers AppointmentView for annotated 400/404 responses.
# Match GlobalExceptionHandler / SecurityConfig / JwtAuthenticationFilter.
spec.setdefault("components", {}).setdefault("schemas", {})["ApiError"] = {
    "type": "object", "required": ["timestamp", "status", "message", "errors"],
    "properties": {
        "timestamp": {"type": "string", "format": "date-time"},
        "status": {"type": "integer", "format": "int32"},
        "message": {"type": "string"},
        "errors": {"type": "object", "additionalProperties": {"type": "string"}},
    },
}
for path, methods in spec["paths"].items():
    for method, operation in methods.items():
        if method not in {"get", "post", "put", "patch", "delete"}:
            continue
        responses = operation["responses"]
        if operation.get("security"):
            responses.setdefault("401", {"description": "Authentication required / invalid token"})
        if path.startswith(("/api/admin/", "/api/doctor/")):
            responses.setdefault("403", {"description": "Role or appointment ownership denied"})
        if path == "/api/auth/login":
            responses.setdefault("401", {"description": "Invalid username or password"})
        for status, response in responses.items():
            if status.isdigit() and int(status) >= 400:
                response["content"] = {"application/json": {"schema": {"$ref": "#/components/schemas/ApiError"}}}
            elif "*/*" in response.get("content", {}):
                response["content"]["application/json"] = response["content"].pop("*/*")
spec["info"]["x-export-note"] = "Generated from /v3/api-docs; error schemas corrected to the shared ApiError response."
examples = {
    ("/api/appointments", "post"): {"patientName": "Nguyen Van A", "phone": "0912345678",
        "serviceId": 2, "preferredDate": "2030-01-02", "preferredTime": "09:00", "note": "Synthetic example"},
    ("/api/auth/login", "post"): {"username": "admin", "password": "Admin123!"},
    ("/api/admin/appointments/{id}/assign", "patch"): {"doctorId": 1, "appointmentDate": "2030-01-02", "startTime": "09:00"},
    ("/api/doctor/appointments/{id}/status", "patch"): {"status": "CONFIRMED"},
    ("/api/admin/appointments/{id}/status", "patch"): {"status": "CANCELLED"},
}
for (path, method), example in examples.items():
    spec["paths"][path][method]["requestBody"]["content"]["application/json"]["example"] = example
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps(spec, indent=2, ensure_ascii=False, sort_keys=True) + "\n", encoding="utf-8")
print(args.output)
