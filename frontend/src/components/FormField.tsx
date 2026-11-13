import type { InputHTMLAttributes, Ref } from 'react'

interface FormFieldProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id' | 'prefix'> {
  id: string
  label: string
  /** Validation message shown under the input. */
  error?: string
  /** Helper text shown under the input. */
  hint?: string
  /** Visual adornment before the value (e.g. `R$`). Hidden from screen readers. */
  prefix?: string
  /** Visual adornment after the value (e.g. `%`). Hidden from screen readers. */
  suffix?: string
  ref?: Ref<HTMLInputElement>
}

/** Labelled input with optional hint, adornments and an accessible error message. */
export function FormField({
  id,
  label,
  error,
  hint,
  prefix,
  suffix,
  ref,
  className,
  ...inputProps
}: FormFieldProps) {
  const hintId = hint ? `${id}-hint` : undefined
  const errorId = error ? `${id}-error` : undefined
  const describedBy = [hintId, errorId].filter(Boolean).join(' ') || undefined

  return (
    <div className={['field', error && 'field--invalid', className].filter(Boolean).join(' ')}>
      <label className="field__label" htmlFor={id}>
        {label}
      </label>
      <div className="field__control">
        {prefix && (
          <span className="field__adornment" aria-hidden="true">
            {prefix}
          </span>
        )}
        <input
          {...inputProps}
          ref={ref}
          id={id}
          name={inputProps.name ?? id}
          className="field__input"
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy}
        />
        {suffix && (
          <span className="field__adornment" aria-hidden="true">
            {suffix}
          </span>
        )}
      </div>
      {hint && (
        <p id={hintId} className="field__hint">
          {hint}
        </p>
      )}
      {error && (
        <p id={errorId} className="field__error">
          {error}
        </p>
      )}
    </div>
  )
}
