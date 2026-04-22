"""
SignLens - translator.py
Translation using deep-translator (Google Translate, no API key needed)
with LibreTranslate as fallback
"""

import logging
import requests

logger = logging.getLogger(__name__)

LANGUAGE_MAP = {
    "english":   "en",
    "kannada":   "kn",
    "telugu":    "te",
    "tamil":     "ta",
    "marathi":   "mr",
    "malayalam": "ml",
    "hindi":     "hi",
    "french":    "fr",
    "spanish":   "es",
    "arabic":    "ar",
}

LIBRETRANSLATE_URL = "https://libretranslate.com/translate"


def _get_code(lang: str) -> str:
    """Accept language name or BCP-47 code, return code"""
    return LANGUAGE_MAP.get(lang.strip().lower(), lang.strip().lower())


def _google(text: str, lang: str) -> str | None:
    try:
        from deep_translator import GoogleTranslator
        return GoogleTranslator(source="auto", target=lang).translate(text)
    except Exception as e:
        logger.warning(f"Google Translate failed: {e}")
        return None


def _libretranslate(text: str, lang: str) -> str | None:
    try:
        resp = requests.post(
            LIBRETRANSLATE_URL,
            json={"q": text, "source": "auto", "target": lang, "format": "text"},
            timeout=10
        )
        resp.raise_for_status()
        return resp.json().get("translatedText")
    except Exception as e:
        logger.warning(f"LibreTranslate failed: {e}")
        return None


def translate(text: str, target_language: str) -> str | None:
    """
    Translate text to target language.
    Tries Google Translate first, falls back to LibreTranslate.
    """
    code = _get_code(target_language)
    result = _google(text, code)
    if result:
        return result
    result = _libretranslate(text, code)
    if result:
        return result
    logger.error(f"All translation providers failed for {code}")
    return None
