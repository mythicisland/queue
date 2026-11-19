package output

import (
	"fmt"
	"io"
	"strings"
	"time"

	"charm.land/lipgloss/v2"
)

var (
	Accent  = lipgloss.NewStyle().Foreground(lipgloss.Cyan)
	Green   = lipgloss.NewStyle().Foreground(lipgloss.Green)
	Yellow  = lipgloss.NewStyle().Foreground(lipgloss.Yellow)
	Red     = lipgloss.NewStyle().Foreground(lipgloss.Red)
	Faint   = lipgloss.NewStyle().Foreground(lipgloss.BrightBlack)
	Bold    = lipgloss.NewStyle().Bold(true)
	Heading = lipgloss.NewStyle().Bold(true)
)

type Step struct {
	Command     string
	Description string
}

func Success(w io.Writer, format string, args ...any) {
	lipgloss.Fprintln(w, Green.Render("✓")+" "+fmt.Sprintf(format, args...))
}

func Info(w io.Writer, format string, args ...any) {
	lipgloss.Fprintln(w, Faint.Render(fmt.Sprintf(format, args...)))
}

func Error(w io.Writer, err error) {
	message := strings.ReplaceAll(err.Error(), "\n", "\n  ")
	lipgloss.Fprintln(w, Red.Render("✗")+" "+message)
}

func Next(w io.Writer, steps ...Step) {
	width := 0
	for _, step := range steps {
		width = max(width, len(step.Command))
	}

	var b strings.Builder
	b.WriteString("\n" + Heading.Render("Next") + "\n")
	for _, step := range steps {
		padding := strings.Repeat(" ", width-len(step.Command)+3)
		b.WriteString("  " + Accent.Render(step.Command) + padding + Faint.Render(step.Description) + "\n")
	}
	lipgloss.Fprint(w, b.String())
}

func Footer(w io.Writer, parts ...string) {
	lipgloss.Fprintln(w, "\n"+Faint.Render(strings.Join(parts, " · ")))
}

func Fields(w io.Writer, fields [][2]string) {
	width := 0
	for _, field := range fields {
		width = max(width, len(field[0]))
	}

	var b strings.Builder
	for _, field := range fields {
		padding := strings.Repeat(" ", width-len(field[0])+3)
		value := strings.ReplaceAll(field[1], "\n", "\n"+strings.Repeat(" ", width+3))
		b.WriteString(Faint.Render(field[0]) + padding + value + "\n")
	}
	lipgloss.Fprint(w, b.String())
}

func Dot(style lipgloss.Style, label string) string {
	return style.Render("●") + " " + label
}

func Active(active bool) string {
	if active {
		return Dot(Green, "active")
	}
	return Faint.Render("○ inactive")
}

func Count(n int, singular string) string {
	if n == 1 {
		return fmt.Sprintf("%d %s", n, singular)
	}
	return fmt.Sprintf("%d %ss", n, singular)
}

func Age(since time.Time) string {
	d := time.Since(since)
	hours := int(d.Hours())
	minutes := int(d.Minutes()) % 60
	seconds := int(d.Seconds()) % 60
	switch {
	case hours > 0:
		return fmt.Sprintf("%dh%dm", hours, minutes)
	case minutes > 0:
		return fmt.Sprintf("%dm%ds", minutes, seconds)
	default:
		return fmt.Sprintf("%ds", max(seconds, 0))
	}
}
