// Where the server is. The client never holds a model key (CLAUDE.md): it only talks to our own server.
export const API_URL: string = process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080';
