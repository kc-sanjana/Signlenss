import os
import base64
import logging
from PIL import Image
import io

logger = logging.getLogger(__name__)

def _pil_to_b64(pil_image: Image.Image) -> str:
    buf = io.BytesIO()
    pil_image.save(buf, format="JPEG", quality=85)
    return base64.standard_b64encode(buf.getvalue()).decode("utf-8")

def describe_sign(pil_image: Image.Image) -> str | None:
    """Use Claude vision to identify all signs in the image."""
    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        return None
    try:
        import anthropic
        client = anthropic.Anthropic(api_key=api_key)
        b64 = _pil_to_b64(pil_image)
        msg = client.messages.create(
            model="claude-haiku-4-5-20251001",
            max_tokens=200,
            messages=[{
                "role": "user",
                "content": [
                    {
                        "type": "image",
                        "source": {"type": "base64", "media_type": "image/jpeg", "data": b64}
                    },
                    {
                        "type": "text",
                        "text": (
                            "Look at this image and identify ALL road signs or street signs visible. "
                            "For each sign: if it has text, give the exact text. "
                            "If it is a symbol (arrow, pedestrian, etc.), describe it briefly. "
                            "List all signs separated by ' | '. "
                            "Examples: 'STOP | Turn Right Arrow' or 'Speed Limit 50 | No Parking' or 'Pedestrian Crossing'. "
                            "Reply with ONLY the sign descriptions, nothing else."
                        )
                    }
                ]
            }]
        )
        result = msg.content[0].text.strip()
        return result if result else None
    except Exception as e:
        logger.warning(f"Vision API failed: {e}")
        return None
