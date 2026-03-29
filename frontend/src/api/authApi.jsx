import axiosInstance from './axiosInstance'

export const registerUser = async (email, password, fullName) => {
  const response = await axiosInstance.post('/api/auth/register', {
    email,
    password,
    fullName,
  })
  return response.data
}

export const loginUser = async (email, password) => {
  const response = await axiosInstance.post('/api/auth/login', {
    email,
    password,
  })
  return response.data
}
