package editor

import (
	"fmt"
	"strings"

	"charm.land/bubbles/v2/textarea"
	tea "charm.land/bubbletea/v2"
	"charm.land/lipgloss/v2"
	"github.com/mythicisland/queue/cli/internal/output"
)

const indent = "  "

type savedMsg struct {
	err error
}

type model struct {
	title          string
	original       string
	area           textarea.Model
	save           Save
	width          int
	height         int
	err            error
	saving         bool
	saved          bool
	confirmingQuit bool
}

func editBuiltIn(title string, content []byte, save Save) (bool, error) {
	area := textarea.New()
	area.MaxHeight = 9999
	area.Prompt = " "
	area.ShowLineNumbers = true
	area.SetStyles(styles())
	area.SetValue(string(content))
	area.MoveToBegin()
	area.Focus()

	initial := model{title: title, original: area.Value(), area: area, save: save}
	result, err := tea.NewProgram(initial).Run()
	if err != nil {
		return false, err
	}
	final, _ := result.(model)
	return final.saved, nil
}

func styles() textarea.Styles {
	s := textarea.DefaultStyles(true)
	s.Focused.CursorLine = lipgloss.NewStyle()
	s.Focused.LineNumber = output.Faint
	s.Focused.CursorLineNumber = output.Accent
	s.Focused.EndOfBuffer = output.Faint
	s.Focused.Prompt = output.Faint
	s.Cursor.Color = lipgloss.Cyan
	return s
}

func (m model) Init() tea.Cmd {
	return textarea.Blink
}

func (m model) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		m.width = msg.Width
		m.height = msg.Height
		m.layout()
		return m, nil

	case savedMsg:
		m.saving = false
		if msg.err != nil {
			m.err = msg.err
			m.layout()
			return m, nil
		}
		m.saved = true
		return m, tea.Quit

	case tea.KeyPressMsg:
		if m.saving {
			return m, nil
		}
		return m.handleKey(msg)
	}

	var cmd tea.Cmd
	m.area, cmd = m.area.Update(msg)
	return m, cmd
}

func (m model) handleKey(msg tea.KeyPressMsg) (tea.Model, tea.Cmd) {
	switch msg.String() {
	case "ctrl+s":
		value := m.area.Value()
		if value == m.original {
			return m, tea.Quit
		}
		m.saving = true
		m.confirmingQuit = false
		save := m.save
		content := []byte(strings.TrimRight(value, "\n") + "\n")
		return m, func() tea.Msg {
			return savedMsg{err: save(content)}
		}

	case "esc", "ctrl+q", "ctrl+c":
		if !m.modified() || m.confirmingQuit {
			return m, tea.Quit
		}
		m.confirmingQuit = true
		m.layout()
		return m, nil

	case "tab":
		m.confirmingQuit = false
		m.area.InsertString(indent)
		return m, nil

	case "enter":
		m.confirmingQuit = false
		m.area.InsertString("\n" + m.nextIndent())
		return m, nil
	}

	if m.confirmingQuit {
		m.confirmingQuit = false
		m.layout()
	}
	var cmd tea.Cmd
	m.area, cmd = m.area.Update(msg)
	return m, cmd
}

func (m model) nextIndent() string {
	lines := strings.Split(m.area.Value(), "\n")
	row := m.area.Line()
	if row >= len(lines) {
		return ""
	}
	line := []rune(lines[row])
	beforeCursor := string(line[:min(m.area.Column(), len(line))])

	current := beforeCursor[:len(beforeCursor)-len(strings.TrimLeft(beforeCursor, " "))]
	trimmed := strings.TrimSpace(beforeCursor)
	if strings.HasSuffix(trimmed, ":") || trimmed == "-" {
		return current + indent
	}
	if strings.HasPrefix(trimmed, "- ") && strings.Contains(trimmed, ":") {
		return current + indent
	}
	return current
}

func (m model) modified() bool {
	return m.area.Value() != m.original
}

func (m *model) layout() {
	m.area.SetWidth(m.width)
	m.area.SetHeight(max(1, m.height-lipgloss.Height(m.header())-lipgloss.Height(m.footer())))
}

func (m model) View() tea.View {
	view := tea.NewView(lipgloss.JoinVertical(lipgloss.Left, m.header(), m.area.View(), m.footer()))
	view.AltScreen = true
	view.WindowTitle = m.title
	return view
}

func (m model) header() string {
	title := output.Accent.Bold(true).Render(" " + m.title)
	if m.modified() {
		title += output.Yellow.Render("  ● modified")
	}
	return title + "\n"
}

func (m model) footer() string {
	wrap := lipgloss.NewStyle().Width(max(1, m.width-2)).PaddingLeft(1)

	var lines []string
	if m.err != nil {
		lines = append(lines, wrap.Inherit(output.Red).Render("✗ "+m.err.Error()))
	}

	position := output.Faint.Render(fmt.Sprintf("Ln %d, Col %d", m.area.Line()+1, m.area.Column()+1))
	var status string
	switch {
	case m.saving:
		status = output.Accent.Render("Saving…")
	case m.confirmingQuit:
		status = output.Yellow.Render("Unsaved changes. Press esc again to discard them or ctrl+s to save.")
	default:
		status = keyHelp("ctrl+s", "save") + "   " + keyHelp("esc", "quit") + "   " + keyHelp("tab", "indent")
	}
	gap := max(1, m.width-lipgloss.Width(status)-lipgloss.Width(position)-2)
	lines = append(lines, " "+status+strings.Repeat(" ", gap)+position)
	return "\n" + strings.Join(lines, "\n")
}

func keyHelp(key, description string) string {
	return output.Bold.Render(key) + " " + output.Faint.Render(description)
}
