package types

import (
	queuev1 "buf.build/gen/go/mythicisland/queue/protocolbuffers/go/mythicisland/queue/v1"
	"github.com/mythicisland/queue/cli/internal/client"
	"github.com/spf13/cobra"
)

func NewCommand() *cobra.Command {
	cmd := &cobra.Command{
		Use:     "types",
		Aliases: []string{"type"},
		Short:   "Manage queue types",
	}
	cmd.AddCommand(
		newListCommand(),
		newGetCommand(),
		newCreateCommand(),
		newEditCommand(),
		newDeleteCommand(),
		newActivateCommand(),
		newDeactivateCommand(),
	)
	return cmd
}

func completeNames(cmd *cobra.Command, args []string, _ string) ([]string, cobra.ShellCompDirective) {
	if len(args) > 0 {
		return nil, cobra.ShellCompDirectiveNoFileComp
	}

	c, err := client.Connect(cmd)
	if err != nil {
		return nil, cobra.ShellCompDirectiveError
	}
	defer c.Close()

	res, err := c.Data.ListQueueTypes(cmd.Context(), &queuev1.ListQueueTypesRequest{})
	if err != nil {
		return nil, cobra.ShellCompDirectiveError
	}

	names := make([]string, 0, len(res.GetQueueTypes()))
	for _, queueType := range res.GetQueueTypes() {
		names = append(names, queueType.GetName())
	}
	return names, cobra.ShellCompDirectiveNoFileComp
}
