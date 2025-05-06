import axios from 'axios';

export interface Case {
  id?: number;
  caseNumber: string;
  caseReason: string;
  status: string;
  parties?: string;
  agents?: string;
  filingDate?: string;
  court?: string;
  procedure?: string;
}

const api = axios.create({
  baseURL: 'http://localhost:8080/api'
});

export function getCases() {
  return api.get<Case[]>('/cases');
}

export function createCase(data: Case) {
  return api.post('/cases', data);
}

export function deleteCase(id: number) {
  return api.delete(`/cases/${id}`);
}

export function updateCase(id: number, data: Case) {
  return api.put(`/cases/${id}`, data);
}