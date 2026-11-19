package matches

import (
	queuev1 "buf.build/gen/go/mythicisland/queue/protocolbuffers/go/mythicisland/queue/v1"
	"github.com/mythicisland/queue/cli/internal/client"
	"github.com/spf13/cobra"
)

func NewCommand() *cobra.Command {
	cmd := &cobra.Command{
		Use:     "matches",
		Aliases: []string{"match"},
		Short:   "See the matches the queue is running",
	}
	cmd.AddCommand(newListCommand(), newGetCommand())
	return cmd
}

func players(match *queuev1.Match) int {
	count := 0
	for _, ticket := range match.GetTickets() {
		count += len(ticket.GetPlayerIds())
	}
	return count
}

func server(match *queuev1.Match) string {
	if !match.HasAssignment() {
		return ""
	}
	return match.GetAssignment().GetServerName()
}

func completeIDs(cmd *cobra.Command, args []string, _ string) ([]string, cobra.ShellCompDirective) {
	if len(args) > 0 {
		return nil, cobra.ShellCompDirectiveNoFileComp
	}

	c, err := client.Connect(cmd)
	if err != nil {
		return nil, cobra.ShellCompDirectiveError
	}
	defer c.Close()

	res, err := c.Data.ListMatches(cmd.Context(), &queuev1.ListMatchesRequest{})
	if err != nil {
		return nil, cobra.ShellCompDirectiveError
	}

	ids := make([]string, 0, len(res.GetMatches()))
	for _, match := range res.GetMatches() {
		ids = append(ids, match.GetId())
	}
	return ids, cobra.ShellCompDirectiveNoFileComp
}
