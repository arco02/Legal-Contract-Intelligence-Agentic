import { useState, useCallback } from 'react'
import { askQuestion, searchContracts, fetchMessages } from '../api/chatApi'

export default function useChat() {
  const [messages,         setMessages]         = useState([])
  const [streamingContent, setStreamingContent] = useState('')
  const [isStreaming,      setIsStreaming]       = useState(false)
  const [isLoading,        setIsLoading]         = useState(false)
  const [conversationId,   setConversationId]   = useState(null)
  const [searchResults,    setSearchResults]    = useState([])

  // ── Load existing conversation messages ──────────────────────────────────
  const loadConversation = useCallback(async (convId) => {
    setIsLoading(true)
    try {
      const data = await fetchMessages(convId)
      // Parse sources JSON string → object if needed
      const parsed = data.map((m) => ({
        ...m,
        sources: typeof m.sources === 'string'
          ? JSON.parse(m.sources || '[]')
          : (m.sources || []),
      }))
      setMessages(parsed)
      setConversationId(convId)
    } catch {
      setMessages([])
    } finally {
      setIsLoading(false)
    }
  }, [])

  // ── ASK mode — streaming ──────────────────────────────────────────────────
  const ask = useCallback((question, docId) => {
    if (isStreaming) return

    // Optimistically add user message
    const userMsg = { id: Date.now(), role: 'user', content: question, sources: [] }
    setMessages((prev) => [...prev, userMsg])
    setStreamingContent('')
    setIsStreaming(true)

    askQuestion(
      question,
      conversationId,
      docId,
      // onToken
      (token) => {
        setStreamingContent((prev) => prev + token)
      },
      // onDone
      (payload) => {
        const assistantMsg = {
          id:          payload.messageId || Date.now() + 1,
          role:        'assistant',
          content:     '', // will be set from streamingContent via ChatWindow
          sources:     payload.sources || [],
          isWebSearch: payload.isWebSearch || false,
        }
        setMessages((prev) => {
          // Replace streaming placeholder with final message
          // ChatWindow reads streamingContent while streaming,
          // then we commit it here as the final message content
          return [...prev, { ...assistantMsg, content: streamingContentRef.current }]
        })
        if (payload.conversationId) {
          setConversationId(payload.conversationId)
        }
        setStreamingContent('')
        setIsStreaming(false)
      },
      // onError
      (err) => {
        setMessages((prev) => [...prev, {
          id:      Date.now() + 1,
          role:    'assistant',
          content: 'Something went wrong. Please try again.',
          sources: [],
        }])
        setStreamingContent('')
        setIsStreaming(false)
      }
    )
  }, [conversationId, isStreaming])

  // ── SEARCH mode ───────────────────────────────────────────────────────────
  const search = useCallback(async (query) => {
    setIsLoading(true)
    setSearchResults([])
    try {
      const data = await searchContracts(query)
      setSearchResults(data.results || [])
    } catch {
      setSearchResults([])
    } finally {
      setIsLoading(false)
    }
  }, [])

  // ── Reset for new conversation ────────────────────────────────────────────
  const reset = useCallback(() => {
    setMessages([])
    setStreamingContent('')
    setIsStreaming(false)
    setConversationId(null)
    setSearchResults([])
  }, [])

  return {
    messages,
    streamingContent,
    isStreaming,
    isLoading,
    conversationId,
    searchResults,
    ask,
    search,
    reset,
    loadConversation,
  }
}

// Ref trick to capture latest streamingContent inside the onDone closure
// without stale closure issues — we expose this as a module-level ref
// that ChatWindow writes to on every token.
export const streamingContentRef = { current: '' }
