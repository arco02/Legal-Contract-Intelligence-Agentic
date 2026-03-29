import { useEffect, useState } from 'react';
import { useParams, useNavigate }       from 'react-router-dom';
import Sidebar       from '../components/layout/Sidebar';
import ChatWindow    from '../components/chat/ChatWindow';
import ChatInput     from '../components/chat/ChatInput';
import ModeToggle    from '../components/chat/ModeToggle';
import { searchContracts } from '../api/chatApi';
import useChat       from '../hooks/useChat';

export default function ChatPage() {
  const { conversationId } = useParams();
  const navigate           = useNavigate();

  const [mode, setMode]                   = useState('ASK');
  const [searchResults, setSearchResults] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [selectedDocId, setSelectedDocId] = useState(null);

  const {
    messages,
    streamingContent,
    isStreaming,
    isLoading,
    conversationId: currentConversationId,
    ask,
    reset,
    loadConversation,
  } = useChat();

  // Sync URL when a brand new conversation is created from the first message
  useEffect(() => {
    if (currentConversationId && currentConversationId !== conversationId) {
      navigate(`/chat/${currentConversationId}`, { replace: true });
    }
  }, [currentConversationId, conversationId, navigate]);

  // ONLY load from DB if the user clicked a link in the sidebar (URL ID changed)
  useEffect(() => {
    if (!conversationId) {
      reset();
      setSearchResults([]);
    } else if (conversationId !== currentConversationId) {
      loadConversation(conversationId);
    }
  }, [conversationId]); 


  const handleSubmit = async (text) => {
    if (!text.trim()) return;

    if (mode === 'ASK') {
      ask(text, selectedDocId);
    } else {
      setSearchLoading(true);
      setSearchResults([]);
      try {
        const results = await searchContracts(text);
        setSearchResults(results.results || results);
      } catch (err) {
        console.error('Search failed:', err);
      } finally {
        setSearchLoading(false);
      }
    }
  };

  return (
    <div style={{ display: 'flex', height: '100vh', background: '#f8f9fa' }}>
      <Sidebar 
        selectedDocumentId={selectedDocId} 
        onDocumentSelect={setSelectedDocId} 
      />

      <main style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        <div style={{
          padding: '0.875rem 1.5rem',
          borderBottom: '1px solid #e9ecef',
          background: '#fff',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexShrink: 0,
        }}>
          <div>
            <span style={{ fontWeight: 600, color: '#1a1a2e', fontSize: '1rem' }}>
              Legal Contract Assistant
            </span>
            <span style={{ marginLeft: '0.75rem', fontSize: '0.75rem', color: '#6c757d', background: '#f1f3f5', padding: '0.2rem 0.6rem', borderRadius: 20 }}>
              Informational only — not legal advice
            </span>
          </div>
          <ModeToggle mode={mode} onChange={setMode} />
        </div>

        <div style={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          {mode === 'ASK' ? (
            <ChatWindow
              messages={messages}
              streamingContent={streamingContent}
              isStreaming={isStreaming}
              isLoading={isLoading}
              mode={mode}
              searchResults={searchResults}
            />
          ) : (
            <SearchResults results={searchResults} loading={searchLoading} />
          )}
        </div>

        <div style={{ padding: '1rem 1.5rem', borderTop: '1px solid #e9ecef', background: '#fff', flexShrink: 0 }}>
          <ChatInput
            onSubmit={handleSubmit}
            disabled={isStreaming || searchLoading}
            placeholder={ mode === 'ASK' ? 'Ask a question about your contracts...' : 'Search for clauses or terms...' }
          />
          <p style={{ fontSize: '0.72rem', color: '#adb5bd', margin: '0.5rem 0 0', textAlign: 'center' }}>
            Answers cite exact contract pages and Indian law statutes. Always verify with a qualified lawyer.
          </p>
        </div>
      </main>
    </div>
  );
}

function SearchResults({ results, loading }) {
  if (loading) {
    return (
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#6c757d' }}>
        <div>
          <div style={{ width: 32, height: 32, border: '3px solid #e9ecef', borderTopColor: '#4f46e5', borderRadius: '50%', animation: 'spin 0.8s linear infinite', margin: '0 auto 0.75rem' }} />
          Searching contracts...
        </div>
      </div>
    );
  }

  if (!results || results.length === 0) {
    return (
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#6c757d', flexDirection: 'column', gap: '0.5rem' }}>
        <span style={{ fontSize: '2rem' }}>🔍</span>
        <p style={{ margin: 0 }}>Enter a query above to search your contracts.</p>
      </div>
    );
  }

  return (
    <div style={{ flex: 1, overflowY: 'auto', padding: '1.25rem 1.5rem' }}>
      <p style={{ color: '#6c757d', fontSize: '0.85rem', marginBottom: '1rem' }}>
        {results.length} result{results.length !== 1 ? 's' : ''} found
      </p>

      {results.map((result, i) => (
        <div key={i} style={{ background: '#fff', border: '1px solid #e9ecef', borderRadius: 10, padding: '1rem 1.25rem', marginBottom: '0.75rem', borderLeft: '4px solid ' + (result.sourceType === 'LAW' ? '#f59e0b' : '#4f46e5') }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
              <span style={{ fontSize: '0.7rem', fontWeight: 700, color: result.sourceType === 'LAW' ? '#f59e0b' : '#4f46e5', background: result.sourceType === 'LAW' ? '#fffbeb' : '#eef2ff', padding: '0.15rem 0.5rem', borderRadius: 4, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                {result.sourceType === 'LAW' ? '⚖️ Law' : '📄 Contract'}
              </span>
              <span style={{ fontWeight: 600, fontSize: '0.88rem', color: '#1a1a2e' }}>
                {result.documentTitle || result.lawTitle}
              </span>
            </div>
            <div style={{ display: 'flex', gap: '0.75rem', fontSize: '0.78rem', color: '#6c757d' }}>
              {result.pageNumber > 0 && <span>Page {result.pageNumber}</span>}
              {result.sectionReference && <span>{result.sectionReference}</span>}
              {result.contractType && <span style={{ background: '#f1f3f5', padding: '0.1rem 0.4rem', borderRadius: 4, fontSize: '0.7rem' }}>{result.contractType}</span>}
            </div>
          </div>
          <p style={{ margin: 0, fontSize: '0.875rem', color: '#374151', lineHeight: 1.6, whiteSpace: 'pre-wrap', maxHeight: 160, overflow: 'hidden' }}>
            {result.content}
          </p>
        </div>
      ))}
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}