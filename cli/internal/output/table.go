package output

import (
	"charm.land/lipgloss/v2"
	"charm.land/lipgloss/v2/table"
)

var (
	header = lipgloss.NewStyle().Bold(true).Padding(0, 1)
	cell   = lipgloss.NewStyle().Padding(0, 1)
	border = lipgloss.NewStyle().Foreground(lipgloss.Color("8"))
)

func Table(headers []string, rows [][]string) {
	t := table.New().
		Border(lipgloss.RoundedBorder()).
		BorderStyle(border).
		Headers(headers...).
		Rows(rows...).
		StyleFunc(func(row, _ int) lipgloss.Style {
			if row == table.HeaderRow {
				return header
			}
			return cell
		})
	lipgloss.Println(t)
}
