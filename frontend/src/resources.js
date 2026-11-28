// One entry per CRUD resource. The generic CrudPage renders a table and a form from this.
// type: text | number | date | select | id (a numeric foreign key, empty = null)
export const RESOURCES = [
  {
    key: 'families',
    label: 'Families',
    path: '/families',
    defaultSort: { field: 'id', direction: 'asc' },
    sortableFields: ['id', 'familyName'],
    fields: [{ name: 'familyName', label: 'Family name', type: 'text', required: true }],
    filters: [{ name: 'familyName', label: 'Family name', type: 'text' }],
  },
  {
    key: 'locations',
    label: 'Locations',
    path: '/locations',
    defaultSort: { field: 'id', direction: 'asc' },
    sortableFields: ['id', 'name', 'streetAddress'],
    fields: [
      { name: 'name', label: 'Name', type: 'text', required: true },
      { name: 'streetAddress', label: 'Street address', type: 'text' },
    ],
    filters: [
      { name: 'name', label: 'Name', type: 'text' },
      { name: 'streetAddress', label: 'Street address', type: 'text' },
    ],
  },
  {
    key: 'housings',
    label: 'Housings',
    path: '/housings',
    defaultSort: { field: 'id', direction: 'asc' },
    sortableFields: ['id', 'locationId', 'housingType', 'capacity'],
    fields: [
      { name: 'locationId', label: 'Location id', type: 'id' },
      { name: 'housingType', label: 'Type', type: 'select', options: ['house', 'apartment'], required: true },
      { name: 'capacity', label: 'Capacity', type: 'number', required: true },
    ],
    filters: [
      { name: 'locationId', label: 'Location id', type: 'number' },
      { name: 'housingType', label: 'Type', type: 'select', options: ['house', 'apartment'] },
      { name: 'capacityFrom', label: 'Capacity from', type: 'number' },
      { name: 'capacityTo', label: 'Capacity to', type: 'number' },
    ],
  },
  {
    key: 'citizens',
    label: 'Citizens',
    path: '/citizens',
    defaultSort: { field: 'id', direction: 'asc' },
    sortableFields: ['id', 'firstName', 'lastName', 'birthDate', 'deathDate', 'status', 'gender', 'familyId', 'housingId'],
    fields: [
      { name: 'firstName', label: 'First name', type: 'text', required: true },
      { name: 'lastName', label: 'Last name', type: 'text', required: true },
      { name: 'birthDate', label: 'Birth date', type: 'date', required: true },
      { name: 'deathDate', label: 'Death date', type: 'date' },
      { name: 'status', label: 'Status', type: 'select', options: ['alive', 'deceased'], required: true },
      { name: 'gender', label: 'Gender', type: 'select', options: ['female', 'male', 'other'] },
      { name: 'familyId', label: 'Family id', type: 'id' },
      { name: 'housingId', label: 'Housing id', type: 'id' },
    ],
    filters: [
      { name: 'status', label: 'Status', type: 'select', options: ['alive', 'deceased'] },
      { name: 'lastName', label: 'Last name', type: 'text' },
      { name: 'birthDateFrom', label: 'Birth date from', type: 'date' },
      { name: 'birthDateTo', label: 'Birth date to', type: 'date' },
      { name: 'familyId', label: 'Family id', type: 'number' },
      { name: 'housingId', label: 'Housing id', type: 'number' },
    ],
  },
  {
    key: 'companies',
    label: 'Companies',
    path: '/companies',
    defaultSort: { field: 'id', direction: 'asc' },
    sortableFields: ['id', 'locationId', 'name', 'foundedDate', 'dissolvedDate', 'status'],
    fields: [
      { name: 'locationId', label: 'Location id', type: 'id' },
      { name: 'name', label: 'Name', type: 'text', required: true },
      { name: 'foundedDate', label: 'Founded date', type: 'date', required: true },
      { name: 'dissolvedDate', label: 'Dissolved date', type: 'date' },
      { name: 'status', label: 'Status', type: 'select', options: ['active', 'dissolved'], required: true },
    ],
    filters: [
      { name: 'locationId', label: 'Location id', type: 'number' },
      { name: 'name', label: 'Name', type: 'text' },
      { name: 'status', label: 'Status', type: 'select', options: ['active', 'dissolved'] },
      { name: 'foundedDateFrom', label: 'Founded date from', type: 'date' },
      { name: 'foundedDateTo', label: 'Founded date to', type: 'date' },
    ],
  },
  {
    key: 'institutions',
    label: 'Institutions',
    path: '/institutions',
    defaultSort: { field: 'id', direction: 'asc' },
    sortableFields: ['id', 'locationId', 'name', 'type'],
    fields: [
      { name: 'locationId', label: 'Location id', type: 'id' },
      { name: 'name', label: 'Name', type: 'text', required: true },
      { name: 'type', label: 'Type', type: 'select', options: ['high_school', 'college', 'hospital'], required: true },
    ],
    filters: [
      { name: 'locationId', label: 'Location id', type: 'number' },
      { name: 'name', label: 'Name', type: 'text' },
      { name: 'type', label: 'Type', type: 'select', options: ['high_school', 'college', 'hospital'] },
    ],
  },
  {
    key: 'jobs',
    label: 'Jobs',
    path: '/jobs',
    defaultSort: { field: 'id', direction: 'asc' },
    sortableFields: ['id', 'citizenId', 'companyId', 'institutionId', 'title', 'salary', 'startDate', 'endDate', 'status'],
    fields: [
      { name: 'citizenId', label: 'Citizen id', type: 'id', required: true },
      { name: 'companyId', label: 'Company id', type: 'id' },
      { name: 'institutionId', label: 'Institution id', type: 'id' },
      { name: 'title', label: 'Title', type: 'text', required: true },
      { name: 'salary', label: 'Salary', type: 'number', required: true },
      { name: 'startDate', label: 'Start date', type: 'date', required: true },
      { name: 'endDate', label: 'End date', type: 'date' },
      { name: 'status', label: 'Status', type: 'select', options: ['active', 'inactive'], required: true },
    ],
    filters: [
      { name: 'citizenId', label: 'Citizen id', type: 'number' },
      { name: 'companyId', label: 'Company id', type: 'number' },
      { name: 'institutionId', label: 'Institution id', type: 'number' },
      { name: 'title', label: 'Title', type: 'text' },
      { name: 'status', label: 'Status', type: 'select', options: ['active', 'inactive'] },
      { name: 'salaryFrom', label: 'Salary from', type: 'number' },
      { name: 'salaryTo', label: 'Salary to', type: 'number' },
      { name: 'startDateFrom', label: 'Start date from', type: 'date' },
      { name: 'startDateTo', label: 'Start date to', type: 'date' },
      { name: 'endDateFrom', label: 'End date from', type: 'date' },
      { name: 'endDateTo', label: 'End date to', type: 'date' },
    ],
  },
  {
    key: 'education-records',
    label: 'Education records',
    path: '/education-records',
    defaultSort: { field: 'id', direction: 'asc' },
    sortableFields: ['id', 'citizenId', 'institutionId', 'startDate', 'endDate'],
    fields: [
      { name: 'citizenId', label: 'Citizen id', type: 'id', required: true },
      { name: 'institutionId', label: 'Institution id', type: 'id', required: true },
      { name: 'startDate', label: 'Start date', type: 'date', required: true },
      { name: 'endDate', label: 'End date', type: 'date' },
    ],
    filters: [
      { name: 'citizenId', label: 'Citizen id', type: 'number' },
      { name: 'institutionId', label: 'Institution id', type: 'number' },
      { name: 'startDateFrom', label: 'Start date from', type: 'date' },
      { name: 'startDateTo', label: 'Start date to', type: 'date' },
      { name: 'endDateFrom', label: 'End date from', type: 'date' },
      { name: 'endDateTo', label: 'End date to', type: 'date' },
    ],
  },
  {
    key: 'citizen-relationships',
    label: 'Citizen relationships',
    path: '/citizen-relationships',
    defaultSort: { field: 'id', direction: 'asc' },
    sortableFields: ['id', 'citizenId1', 'citizenId2', 'relationshipType', 'startDate', 'endDate'],
    fields: [
      { name: 'citizenId1', label: 'First citizen id', type: 'id', required: true },
      { name: 'citizenId2', label: 'Second citizen id', type: 'id', required: true },
      { name: 'relationshipType', label: 'Relationship type', type: 'select', options: ['parent_child', 'marriage'], required: true },
      { name: 'startDate', label: 'Start date', type: 'date', required: true },
      { name: 'endDate', label: 'End date', type: 'date' },
    ],
    filters: [
      { name: 'citizenId1', label: 'First citizen id', type: 'number' },
      { name: 'citizenId2', label: 'Second citizen id', type: 'number' },
      { name: 'relationshipType', label: 'Relationship type', type: 'select', options: ['parent_child', 'marriage'] },
      { name: 'startDateFrom', label: 'Start date from', type: 'date' },
      { name: 'startDateTo', label: 'Start date to', type: 'date' },
      { name: 'endDateFrom', label: 'End date from', type: 'date' },
      { name: 'endDateTo', label: 'End date to', type: 'date' },
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