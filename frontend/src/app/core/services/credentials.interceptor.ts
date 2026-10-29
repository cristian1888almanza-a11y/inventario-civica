import { HttpInterceptorFn } from '@angular/common/http';

export const credentialsInterceptor: HttpInterceptorFn = (request, next) => {
  const cookie = document.cookie
    .split('; ')
    .find(value => value.startsWith('XSRF-TOKEN='))
    ?.slice('XSRF-TOKEN='.length);
  const csrfToken = cookie ? decodeURIComponent(cookie) : null;
  const isMutation = !['GET', 'HEAD', 'OPTIONS'].includes(request.method);
  const headers = isMutation && csrfToken ? request.headers.set('X-XSRF-TOKEN', csrfToken) : request.headers;
  return next(request.clone({ headers, withCredentials: true }));
};
