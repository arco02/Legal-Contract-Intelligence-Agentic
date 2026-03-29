import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import DocumentUpload from '../components/document/DocumentUpload';
import DocumentList from '../components/document/DocumentList';
import Sidebar from '../components/layout/Sidebar';
import { fetchDocuments } from '../api/documentApi';

export default function DashboardPage() {
  const navigate = useNavigate();
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);

  const loadDocs = async () => {
    try {
      const data = await fetchDocuments();
      setDocuments(data || []);
    } catch (err) {
      console.error('Failed to load documents:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDocs();
  }, []);

  return (
    <div style={{ display: 'flex', height: '100vh', background: '#f8f9fa' }}>
      <Sidebar 
        selectedDocumentId={null} 
        onDocumentSelect={() => {}} 
      />

      <main style={{ flex: 1, overflowY: 'auto', padding: '2rem' }}>
        <div style={{ maxWidth: 800, margin: '0 auto' }}>

          {/* Header */}
          <div style={{ marginBottom: '2rem' }}>
            <h1 style={{ fontSize: '1.75rem', fontWeight: 700, color: '#1a1a2e', margin: 0 }}>
              Contract Dashboard
            </h1>
            <p style={{ color: '#6c757d', marginTop: '0.5rem' }}>
              Upload contracts, then head to Chat to ask questions about them.
            </p>
          </div>

          {/* Quick actions */}
          <div style={{ display: 'flex', gap: '1rem', marginBottom: '2rem' }}>
            <button
              onClick={() => navigate('/chat')}
              style={{
                padding: '0.75rem 1.5rem',
                background: '#4f46e5',
                color: '#fff',
                border: 'none',
                borderRadius: 8,
                fontWeight: 600,
                cursor: 'pointer',
                fontSize: '0.95rem',
              }}>
              💬 Open Chat
            </button>
          </div>

          {/* Upload section */}
          <section style={{
            background: '#fff',
            borderRadius: 12,
            padding: '1.5rem',
            boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
            marginBottom: '1.5rem',
          }}>
            <h2 style={{ fontSize: '1.1rem', fontWeight: 600, margin: '0 0 1rem', color: '#1a1a2e' }}>
              Upload Contract
            </h2>
            <DocumentUpload onUploadSuccess={loadDocs} />
          </section>

          {/* Document list section */}
          <section style={{
            background: '#fff',
            borderRadius: 12,
            padding: '1.5rem',
            boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
          }}>
            <h2 style={{ fontSize: '1.1rem', fontWeight: 600, margin: '0 0 1rem', color: '#1a1a2e' }}>
              Your Contracts
            </h2>
            {loading ? (
              <p style={{ color: 'var(--text-secondary)', fontSize: '14px' }}>Loading documents...</p>
            ) : (
              <DocumentList 
                documents={documents} 
                onDeleted={(id) => setDocuments(docs => docs.filter(d => d.id !== id))} 
              />
            )}
          </section>

        </div>
      </main>
    </div>
  );
}