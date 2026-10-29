import { Component } from '@angular/core';
import { TaskManagerComponent } from './pages/task-manager/task-manager.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [TaskManagerComponent],
  template: '<app-inventory />'
})
export class AppComponent {}
