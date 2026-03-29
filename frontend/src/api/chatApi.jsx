import axiosInstance from './axiosInstance'

export const fetchConversations = async () => {
  const response = await axiosInstance.get('/api/conversations')
  return response.data
}

export const fetchMessages = async (conversationId) => {
  const response = await axiosInstance.get(`/api/conversations/${conversationId}/messages`)
  return response.data
}

export const deleteConversation = async (conversationId) => {
  const response = await axiosInstance.delete(`/api/conversations/${conversationId}`)
  return response.data
}

export const searchContracts = async (query, contractTypes) => {
  const response = await axiosInstance.post('/api/chat/search', {
    query,
    contractTypes: contractTypes || [],
  })
  return response.data
}

export const askQuestion = (
  question,
  conversationId,
  documentId,
  onToken,
  onDone,
  onError
) => {
  const token = localStorage.getItem('jwt')
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

  fetch(`${baseUrl}/api/chat/ask`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      question,
      conversationId: conversationId || null,
      documentId: documentId || null,
    }),
  })
    .then((response) => {
      if (!response.ok) throw new Error(`HTTP ${response.status}`)

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      const pump = () =>
        reader.read().then(({ done, value }) => {
          if (done) {
            // Spring Boot often omits the space after the colon
            if (buffer.trim().startsWith('data:')) {
               try { 
                 const data = JSON.parse(buffer.trim().slice(5).trim());
                 if (data.done) onDone(data);
               } catch {}
            }
            return
          }

          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''

          lines.forEach((line) => {
            if (!line.startsWith('data:')) return
            try {
              // Extract everything after 'data:' and parse
              const jsonStr = line.slice(5).trim()
              const data = JSON.parse(jsonStr)
              
              if (data.error) onError(new Error(data.error))
              else if (data.done) onDone(data)
              else if (data.token) onToken(data.token)
            } catch {
              // Ignore partial JSON, next loop will catch it
            }
          })

          pump()
        })

      pump()
    })
    .catch((err) => onError(err))
}