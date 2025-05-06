import axios from 'axios';

export interface Category {
  id: number;
  name: string;
  parent?: Category;
  children?: Category[];
}

const api = axios.create({
  baseURL: 'http://localhost:8080/api'
});

export function getCategories() {
  return api.get<Category[]>('/categories');
}

