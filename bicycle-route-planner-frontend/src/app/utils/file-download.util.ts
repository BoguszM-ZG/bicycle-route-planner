const POLISH_DIACRITICS_MAP: Readonly<Record<string, string>> = {
  ą: 'a',
  ć: 'c',
  ę: 'e',
  ł: 'l',
  ń: 'n',
  ó: 'o',
  ś: 's',
  ź: 'z',
  ż: 'z',
  Ą: 'A',
  Ć: 'C',
  Ę: 'E',
  Ł: 'L',
  Ń: 'N',
  Ó: 'O',
  Ś: 'S',
  Ź: 'Z',
  Ż: 'Z',
};

const DEFAULT_FILE_NAME = 'route';
const DEFAULT_EXTENSION = 'gpx';


export function sanitizeFileName(rawName: string, extension = DEFAULT_EXTENSION): string {
  const trimmed = (rawName ?? '').trim();
  const normalizedExt = extension ? extension.replace(/^\.+/, '').trim() : '';

  let baseName = trimmed;
  if (normalizedExt && trimmed.toLowerCase().endsWith(`.${normalizedExt.toLowerCase()}`)) {
    baseName = trimmed.slice(0, -(normalizedExt.length + 1)).trim();
  }

  const validBaseName = baseName.length > 0 ? baseName : DEFAULT_FILE_NAME;
  const asciiName = validBaseName.replace(
    /[ąćęłńóśźżĄĆĘŁŃÓŚŹŻ]/g,
    (match) => POLISH_DIACRITICS_MAP[match] ?? match,
  );
  const safeName = asciiName.replace(/[^a-zA-Z0-9-_]/g, '_');

  return normalizedExt ? `${safeName}.${normalizedExt}` : safeName;
}

export function triggerBlobDownload(blob: Blob, fileName: string): void {
  if (!blob || typeof window === 'undefined' || typeof document === 'undefined') {
    return;
  }

  const blobUrl = window.URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = blobUrl;
  anchor.download = fileName;

  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();

  window.URL.revokeObjectURL(blobUrl);
}
