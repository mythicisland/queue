package matches

import (
	"slices"
	"strconv"

	queuev1 "buf.build/gen/go/mythicisland/queue/protocolbuffers/go/mythicisland/queue/v1"
	"github.com/mythicisland/queue/cli/internal/client"
	"github.com/mythicisland/queue/cli/internal/output"
	"github.com/spf13/cobra"
)

func newListCommand() *cobra.Command {
	var queueType, serverName string

	cmd := &cobra.Command{
		Use:     "list",
		Aliases: []string{"ls"},
		Short:   "List all matches, oldest first",
		Example: `  queue matches list
  queue matches list --type bedwars-4x4
  queue matches list --server bedwars-3`,
		Args: cobra.NoArgs,
		RunE: func(cmd *cobra.Command, _ []string) error {
			format, err := output.Format(cmd, output.FormatTable)
			if err != nil {
				return err
			}

			c, err := client.Connect(cmd)
			if err != nil {
				return err
			}
			defer c.Close()

			req := queuev1.ListMatchesRequest_builder{QueueType: queueType}.Build()
			res, err := c.Data.ListMatches(cmd.Context(), req)
			if err != nil {
				return err
			}
			matches := res.GetMatches()
			if serverName != "" {
				matches = slices.DeleteFunc(matches, func(match *queuev1.Match) bool {
					return server(match) != serverName && match.GetAssignment().GetServerId() != serverName
				})
			}
			slices.SortFunc(matches, func(a, b *queuev1.Match) int {
				return a.GetCreatedAt().AsTime().Compare(b.GetCreatedAt().AsTime())
			})

			if format != output.FormatTable {
				return output.PrintList(cmd.OutOrStdout(), matches, format)
			}

			w := cmd.OutOrStdout()
			if len(matches) == 0 {
				output.Info(w, "No matches right now")
				return nil
			}

			total := 0
			rows := make([][]string, 0, len(matches))
			for _, match := range matches {
				total += players(match)
				rows = append(rows, []string{
					match.GetId(),
					match.GetQueueType(),
					output.MatchState(match.GetState()),
					strconv.Itoa(players(match)),
					server(match),
					output.Age(match.GetCreatedAt().AsTime()),
				})
			}
			output.Table(w, []string{"ID", "QUEUE TYPE", "STATE", "PLAYERS", "SERVER", "AGE"}, rows)
			output.Footer(w, output.Count(len(matches), "match"), output.Count(total, "player"))
			output.Next(w, output.Step{Command: "queue matches get <id>", Description: "Show the teams and server of a match"})
			return nil
		},
	}
	cmd.Flags().StringVar(&queueType, "type", "", "only matches of this queue type")
	cmd.Flags().StringVar(&serverName, "server", "", "only matches on this server, by name or id")
	return cmd
}
