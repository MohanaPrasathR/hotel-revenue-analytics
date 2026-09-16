import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ExportModalComponent } from './export-modal.component';
import { FormsModule } from '@angular/forms';

describe('ExportModalComponent', () => {
  let component: ExportModalComponent;
  let fixture: ComponentFixture<ExportModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExportModalComponent, FormsModule]
    }).compileComponents();

    fixture = TestBed.createComponent(ExportModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the export modal component', () => {
    expect(component).toBeTruthy();
  });

  it('should default to CSV format and includeCancelled = true', () => {
    expect(component.selectedFormat).toBe('csv');
    expect(component.includeCancelled).toBeTrue();
  });

  it('should emit close event when onClose is called', () => {
    spyOn(component.close, 'emit');
    component.onClose();
    expect(component.close.emit).toHaveBeenCalled();
  });

  it('should emit export options and then close when onExport is invoked', () => {
    spyOn(component.export, 'emit');
    spyOn(component.close, 'emit');

    component.selectedFormat = 'json';
    component.includeCancelled = false;
    component.onExport();

    expect(component.export.emit).toHaveBeenCalledWith({
      format: 'json',
      includeCancelled: false
    });
    expect(component.close.emit).toHaveBeenCalled();
  });

  it('should close on Escape key press when open', () => {
    component.isOpen = true;
    spyOn(component.close, 'emit');

    component.handleEscapeKey();

    expect(component.close.emit).toHaveBeenCalled();
  });
});
