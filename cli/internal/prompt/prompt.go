package prompt

import (
	"errors"
	"strings"

	"charm.land/huh/v2"
	"charm.land/lipgloss/v2"
	"github.com/mythicisland/queue/cli/internal/output"
)

var Theme = huh.ThemeFunc(func(isDark bool) *huh.Styles {
	t := huh.ThemeBase(isDark)
	button := lipgloss.NewStyle().Padding(0, 2).MarginRight(1)

	t.Focused.Base = t.Focused.Base.BorderForeground(lipgloss.Cyan)
	t.Focused.Card = t.Focused.Base
	t.Focused.Title = output.Bold
	t.Focused.NoteTitle = output.Bold.MarginBottom(1)
	t.Focused.Description = output.Faint
	t.Focused.ErrorIndicator = output.Red.SetString(" *")
	t.Focused.ErrorMessage = output.Red
	t.Focused.SelectSelector = output.Accent.SetString("› ")
	t.Focused.NextIndicator = output.Accent.MarginLeft(1).SetString("→")
	t.Focused.PrevIndicator = output.Accent.MarginRight(1).SetString("←")
	t.Focused.Option = lipgloss.NewStyle()
	t.Focused.SelectedOption = output.Accent
	t.Focused.TextInput.Prompt = output.Accent
	t.Focused.TextInput.Placeholder = output.Faint
	t.Focused.TextInput.Cursor = output.Accent
	t.Focused.FocusedButton = button.Foreground(lipgloss.Black).Background(lipgloss.Cyan)
	t.Focused.BlurredButton = button.Foreground(lipgloss.BrightBlack)
	t.Focused.Next = t.Focused.FocusedButton

	t.Blurred = t.Focused
	t.Blurred.Base = t.Focused.Base.BorderStyle(lipgloss.HiddenBorder())
	t.Blurred.Card = t.Blurred.Base
	t.Blurred.Title = output.Faint
	t.Blurred.SelectSelector = lipgloss.NewStyle().SetString("  ")
	t.Blurred.NextIndicator = lipgloss.NewStyle()
	t.Blurred.PrevIndicator = lipgloss.NewStyle()

	t.Group.Title = output.Accent.Bold(true).MarginBottom(1)
	t.Group.Description = output.Faint.MarginBottom(1)
	return t
})

func Password(title string) (string, error) {
	var value string
	field := huh.NewInput().
		Title(title).
		Prompt(" › ").
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
	err := run(field)
	if errors.Is(err, huh.ErrUserAborted) {
		return false, nil
	}
	if err != nil {
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
		WithTheme(Theme).
		WithShowHelp(false).
		Run()
}

func answered(title, value string) {
	lipgloss.Println(output.Green.Render("✓") + " " + output.Faint.Render(title) + " " + value)
}

func required(value string) error {
	if value == "" {
		return errors.New("required")
	}
	return nil
}
