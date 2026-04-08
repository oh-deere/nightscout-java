import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import { en } from './en'
import { sv } from './sv'

/**
 * i18n bootstrap. Uses react-i18next on top of i18next. The active language is
 * driven by the `language` runtime setting fetched from /api/v1/status — see
 * AppShell, which calls i18n.changeLanguage(...) when status loads.
 *
 * Two bundles ship by default: English (en) and Swedish (sv). Add more by
 * dropping a file under src/i18n/<code>.ts and registering it in `resources`.
 */
void i18n.use(initReactI18next).init({
  resources: {
    en: { translation: en },
    sv: { translation: sv },
  },
  lng: 'en',
  fallbackLng: 'en',
  interpolation: { escapeValue: false }, // React already escapes
  returnNull: false,
})

export default i18n
