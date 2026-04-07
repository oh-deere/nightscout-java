/**
 * SHA-1 hex of a UTF-8 string, using the Web Crypto API. Used to hash the Nightscout API
 * secret in the same way the legacy uploaders do.
 */
export async function sha1Hex(input: string): Promise<string> {
  const data = new TextEncoder().encode(input)
  const buf = await crypto.subtle.digest('SHA-1', data)
  return Array.from(new Uint8Array(buf))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('')
}
