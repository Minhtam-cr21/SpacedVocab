/** Vietnamese formats: dd/MM/yyyy and HH:mm (docs/design/06-ux-copy.md). */
const dateTime = new Intl.DateTimeFormat('vi-VN', {
  day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
});

export const formatDateTime = (iso) => dateTime.format(new Date(iso));

export function formatDays(days) {
  if (days == null) return '';
  if (days < 30) return `${days} ngày`;
  if (days < 365) return `${Math.round(days / 30)} tháng`;
  return `${(days / 365).toFixed(1).replace('.', ',')} năm`;
}
