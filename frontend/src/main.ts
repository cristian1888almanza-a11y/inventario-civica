import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { AppComponent } from './app/app.component';
import { credentialsInterceptor } from './app/core/services/credentials.interceptor';

bootstrapApplication(AppComponent, {
  providers: [provideHttpClient(withInterceptors([credentialsInterceptor]))]
}).catch(console.error);
