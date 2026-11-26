package types

import (
	"fmt"

	queuev1 "buf.build/gen/go/mythicisland/queue/protocolbuffers/go/mythicisland/queue/v1"
	"github.com/mythicisland/queue/cli/internal/client"
	"github.com/mythicisland/queue/cli/internal/output"
	"github.com/mythicisland/queue/cli/internal/prompt"
	"github.com/spf13/cobra"
)

func newDeleteCommand() *cobra.Command {
	var yes bool

	cmd := &cobra.Command{
		Use:     "delete <name>",
		Aliases: []string{"rm"},
		Short:   "Delete a queue type",
		Long: `Delete a queue type. Tickets without any other queue type are deleted, running matches keep going.

To stop a queue type only for a while, deactivate it instead.`,
		Args:              cobra.ExactArgs(1),
		ValidArgsFunction: completeNames,
		RunE: func(cmd *cobra.Command, args []string) error {
			name := args[0]
			if !yes {
				confirmed, err := prompt.Confirm(fmt.Sprintf("Delete queue type %s?", name), "Delete")
				if err != nil || !confirmed {
					return err
				}
			}

			c, err := client.Connect(cmd)
			if err != nil {
				return err
			}
			defer c.Close()

			if _, err := c.Types.DeleteQueueType(cmd.Context(), queuev1.DeleteQueueTypeRequest_builder{Name: name}.Build()); err != nil {
				return err
			}
			output.Success(cmd.OutOrStdout(), "Deleted queue type %s", name)
			return nil
		},
	}
	cmd.Flags().BoolVarP(&yes, "yes", "y", false, "delete without asking")
	return cmd
}
