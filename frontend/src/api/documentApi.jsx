import axiosInstance from './axiosInstance'

export const uploadDocument = async (file, contractType, title) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('contractType', contractType)
  formData.append('title', title)

  const response = await axiosInstance.post('/api/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return response.data
}

export const fetchDocuments = async () => {
  const response = await axiosInstance.get('/api/documents')
  return response.data
}

export const deleteDocument = async (documentId) => {
  const response = await axiosInstance.delete(`/api/documents/${documentId}`)
  return response.data
}
