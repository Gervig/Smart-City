// One entry per CRUD resource. The generic CrudPage renders a table and a form from this.
// type: text | number | date | select | id (a numeric foreign key, empty = null)
export const RESOURCES = [
  {
    key: 'families',
    label: 'Families',
    path: '/families',
    fields: [{ name: 'familyName', label: 'Family name', type: 'text', required: true }],
  },
  {
    key: 'locations',
    label: 'Locations',
    path: '/locations',
    fields: [
      { name: 'name', label: 'Name', type: 'text', required: true },
      { name: 'streetAddress', label: 'Street address', type: 'text' },
    ],
  },
  {
    key: 'housings',
    label: 'Housings',
    path: '/housings',
    fields: [
      { name: 'locationId', label: 'Location id', type: 'id' },
      { name: 'housingType', label: 'Type', type: 'select', options: ['house', 'apartment'], required: true },
      { name: 'capacity', label: 'Capacity', type: 'number', required: true },
    ],
  },
  {
    key: 'citizens',
    label: 'Citizens',
    path: '/citizens',
    fields: [
      { name: 'firstName', label: 'First name', type: 'text', required: true },
      { name: 'lastName', label: 'Last name', type: 'text', required: true },
      { name: 'birthDate', label: 'Birth date', type: 'date', required: true },
      { name: 'deathDate', label: 'Death date', type: 'date' },
      { name: 'status', label: 'Status', type: 'select', options: ['alive', 'dead'], required: true },
      { name: 'gender', label: 'Gender', type: 'select', options: ['female', 'male', 'other'] },
      { name: 'familyId', label: 'Family id', type: 'id' },
      { name: 'housingId', label: 'Housing id', type: 'id' },
    ],
  },
]

export function emptyForm(resource)
{
  const form = {}
  for (const f of resource.fields)
  {
    form[f.name] = f.type === 'select' ? (f.options[0] ?? '') : ''
  }
  return form
}

// Convert the string values in the form to what the API expects.
export function toPayload(resource, form)
{
  const body = {}
  for (const f of resource.fields)
  {
    const v = form[f.name]
    if (f.type === 'number') body[f.name] = v === '' ? 0 : Number(v)
    else if (f.type === 'id') body[f.name] = v === '' ? null : Number(v)
    else if (f.type === 'date') body[f.name] = v === '' ? null : v
    else body[f.name] = v
  }
  return body
}

// Jackson may serialise LocalDate as [yyyy, m, d]. Show and edit it as yyyy-mm-dd either way.
export function formatValue(field, value)
{
  if (value === null || value === undefined) return ''
  if (field.type === 'date' && Array.isArray(value) && value.length === 3)
  {
    const [y, m, d] = value
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
  }
  return value
}

export function fromRecord(resource, record)
{
  const form = {}
  for (const f of resource.fields) form[f.name] = formatValue(f, record[f.name])
  return form
}
