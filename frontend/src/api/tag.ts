import axios from 'axios';

export interface Tag {
  id: number;
  name: string;
}

const api = axios.create({
  baseURL: 'http://localhost:8080/api'
});

export function getTags() {
  return api.get<Tag[]>('/tags');
}
