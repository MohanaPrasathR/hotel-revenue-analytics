import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-export-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './export-modal.component.html',
  styleUrls: ['./export-modal.component.css']
})
export class ExportModalComponent {
  @Input() isOpen = false;
  @Output() close = new EventEmitter<void>();
  @Output() export = new EventEmitter<{ format: 'csv' | 'json'; includeCancelled: boolean }>();

  selectedFormat: 'csv' | 'json' = 'csv';
  includeCancelled = true;

  onClose(): void {
    this.close.emit();
  }

  onExport(): void {
    this.export.emit({
      format: this.selectedFormat,
      includeCancelled: this.includeCancelled
    });
    this.onClose();
  }
}
