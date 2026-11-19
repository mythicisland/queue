package output

import (
	"io"

	"charm.land/lipgloss/v2"
	"charm.land/lipgloss/v2/table"
)

var (
	header = Faint.PaddingRight(3)
	cell   = lipgloss.NewStyle().PaddingRight(3)
)

func Table(w io.Writer, headers []string, rows [][]string) {
	t := table.New().
		BorderTop(false).
		BorderBottom(false).
		BorderLeft(false).
		BorderRight(false).
		BorderHeader(false).
		BorderColumn(false).
		BorderRow(false).
		Headers(headers...).
		Rows(rows...).
		StyleFunc(func(row, _ int) lipgloss.Style {
			if row == table.HeaderRow {
				return header
			}
			return cell
		})
	lipgloss.Fprintln(w, t)
}
