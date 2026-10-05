"""
Security dependency for internal service-to-service authentication.
Validates the X-Internal-Key header against the configured API_KEY.
"""

from fastapi import Depends, HTTPException, Security
from fastapi.security import APIKeyHeader
from starlette.status import HTTP_401_UNAUTHORIZED

from app.core.config import Settings, get_settings

api_key_header = APIKeyHeader(name="X-Internal-Key", auto_error=False)


async def verify_internal_key(
    api_key: str | None = Security(api_key_header),
    settings: Settings = Depends(get_settings),
) -> str:
    """
    Dependency that validates the internal API key.
    
    Checks the X-Internal-Key header against the configured API_KEY.
    Returns the validated key on success, raises 401 on failure.
    """
    if api_key is None:
        raise HTTPException(
            status_code=HTTP_401_UNAUTHORIZED,
            detail="Missing X-Internal-Key header",
        )
    if api_key != settings.API_KEY:
        raise HTTPException(
            status_code=HTTP_401_UNAUTHORIZED,
            detail="Invalid internal API key",
        )
    return api_key
