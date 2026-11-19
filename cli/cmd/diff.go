package cmd

import (
	"fmt"
	"strings"

	queuev1 "buf.build/gen/go/mythicisland/queue/protocolbuffers/go/mythicisland/queue/v1"
	"charm.land/lipgloss/v2"
	"github.com/aymanbagabas/go-udiff"
	"github.com/mythicisland/queue/cli/internal/client"
	"github.com/mythicisland/queue/cli/internal/manifest"
	"github.com/mythicisland/queue/cli/internal/plan"
	"github.com/spf13/cobra"
)

var (
	addedLine   = lipgloss.NewStyle().Foreground(lipgloss.Green)
	removedLine = lipgloss.NewStyle().Foreground(lipgloss.Red)
	hunkLine    = lipgloss.NewStyle().Foreground(lipgloss.Cyan)
)

func newDiffCommand() *cobra.Command {
	var file string
	var prune bool

	cmd := &cobra.Command{
		Use:     "diff",
		Short:   "Show what apply would change",
		Example: `  queue diff -f types/ --prune`,
		Args:    cobra.NoArgs,
		RunE: func(cmd *cobra.Command, _ []string) error {
			wanted, err := manifest.Load(file)
			if err != nil {
				return err
			}

			c, err := client.Connect(cmd)
			if err != nil {
				return err
			}
			defer c.Close()

			changes, err := plan.Make(cmd.Context(), c, wanted, prune)
			if err != nil {
				return err
			}

			changed := false
			for _, change := range changes {
				if change.Action == plan.Unchanged {
					continue
				}
				changed = true

				diff, err := unified(change)
				if err != nil {
					return err
				}
				lipgloss.Fprintln(cmd.OutOrStdout(), actionStyles[change.Action].Bold(true).Render(fmt.Sprintf("%s %s", change.Action, change.Name())))
				lipgloss.Fprintln(cmd.OutOrStdout(), colorize(diff))
			}
			if !changed {
				fmt.Fprintln(cmd.OutOrStdout(), "No changes")
			}
			return nil
		},
	}
	cmd.Flags().StringVarP(&file, "file", "f", "", "file or directory with queue types, - for stdin")
	cmd.Flags().BoolVar(&prune, "prune", false, "also show queue types that have no file")
	_ = cmd.MarkFlagRequired("file")
	return cmd
}

func unified(change plan.Change) (string, error) {
	current, err := yamlOf(change.Current)
	if err != nil {
		return "", err
	}
	wanted, err := yamlOf(change.Wanted)
	if err != nil {
		return "", err
	}
	return udiff.Unified("queue", "files", current, wanted), nil
}

func yamlOf(queueType *queuev1.QueueType) (string, error) {
	if queueType == nil {
		return "", nil
	}
	data, err := manifest.Encode(queueType, manifest.YAML)
	return string(data), err
}

func colorize(diff string) string {
	lines := strings.Split(strings.TrimSuffix(diff, "\n"), "\n")
	for i, line := range lines {
		switch {
		case strings.HasPrefix(line, "+++"), strings.HasPrefix(line, "---"):
			lines[i] = lipgloss.NewStyle().Faint(true).Render(line)
		case strings.HasPrefix(line, "+"):
			lines[i] = addedLine.Render(line)
		case strings.HasPrefix(line, "-"):
			lines[i] = removedLine.Render(line)
		case strings.HasPrefix(line, "@@"):
			lines[i] = hunkLine.Render(line)
		}
	}
	return strings.Join(lines, "\n")
}
