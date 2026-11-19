package prompt

import (
	"errors"
	"strings"

	"charm.land/huh/v2"
	"charm.land/lipgloss/v2"
	"github.com/mythicisland/queue/cli/internal/output"
)

var theme = huh.ThemeFunc(func(bool) *huh.Styles {
	t := huh.ThemeBase(false)
	t.FieldSeparator = lipgloss.NewStyle().SetString("\n")

	t.Focused.Base = lipgloss.NewStyle()
	t.Focused.Card = t.Focused.Base
	t.Focused.Title = output.Accent.Bold(true).SetString("?")
	t.Focused.Description = output.Faint
	t.Focused.ErrorIndicator = output.Red.SetString(" *")
	t.Focused.ErrorMessage = output.Red
	t.Focused.SelectSelector = output.Accent.SetString("› ")
	t.Focused.Option = lipgloss.NewStyle()
	t.Focused.SelectedOption = output.Accent
	t.Focused.TextInput.Prompt = output.Faint
	t.Focused.TextInput.Placeholder = output.Faint
	t.Focused.TextInput.Cursor = output.Accent
	t.Focused.FocusedButton = lipgloss.NewStyle().Padding(0, 2).MarginRight(1).Foreground(lipgloss.Black).Background(lipgloss.Cyan)
	t.Focused.BlurredButton = lipgloss.NewStyle().Padding(0, 2).MarginRight(1).Foreground(lipgloss.BrightBlack)

	t.Blurred = t.Focused
	return t
})

func Input(title, placeholder string, validate func(string) error) (string, error) {
	var value string
	field := huh.NewInput().
		Title(title).
		Placeholder(placeholder).
		Prompt(" ").
		Inline(true).
		Value(&value)
	if validate != nil {
		field.Validate(validate)
	}
	if err := run(field); err != nil {
		return "", err
	}
	answered(title, value)
	return value, nil
}

func Password(title string) (string, error) {
	var value string
	field := huh.NewInput().
		Title(title).
		Prompt(" ").
		Inline(true).
		EchoMode(huh.EchoModePassword).
		Validate(required).
		Value(&value)
	if err := run(field); err != nil {
		return "", err
	}
	answered(title, strings.Repeat("•", 8))
	return value, nil
}

func Select[T comparable](title string, options ...huh.Option[T]) (T, error) {
	var value T
	field := huh.NewSelect[T]().
		Title(title).
		Options(options...).
		Value(&value)
	if err := run(field); err != nil {
		return value, err
	}
	for _, option := range options {
		if option.Value == value {
			answered(title, option.Key)
		}
	}
	return value, nil
}

func Confirm(title, affirmative string) (bool, error) {
	var value bool
	field := huh.NewConfirm().
		Title(title).
		Affirmative(affirmative).
		Negative("Cancel").
		Inline(true).
		Value(&value)
	if err := run(field); err != nil {
		return false, err
	}
	if value {
		answered(title, affirmative)
	} else {
		answered(title, "Cancel")
	}
	return value, nil
}

func run(field huh.Field) error {
	return huh.NewForm(huh.NewGroup(field)).
		WithTheme(theme).
		WithShowHelp(false).
		Run()
}

func answered(title, value string) {
	lipgloss.Println(output.Accent.Render("?") + " " + output.Bold.Render(title) + " " + output.Accent.Render(value))
}

func required(value string) error {
	if value == "" {
		return errors.New("required")
	}
	return nil
}
