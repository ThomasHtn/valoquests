import { Environment } from './environment.model';

/**
 * Development configuration.
 *
 * `/api` is proxied to the local backend by `proxy.conf.json`, so the frontend and the API stay
 * same-origin during development and no CORS configuration is required.
 */
export const environment: Environment = {
  apiBaseUrl: '/api',
};
