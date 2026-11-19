package types

import (
	"fmt"

	queuev1 "buf.build/gen/go/mythicisland/queue/protocolbuffers/go/mythicisland/queue/v1"
	"github.com/mythicisland/queue/cli/internal/client"
	"github.com/spf13/cobra"
)

func newActivateCommand() *cobra.Command {
	return newSetActiveCommand("activate", "Let a queue type take tickets again", "Activated", true)
}

func newDeactivateCommand() *cobra.Command {
	cmd := newSetActiveCommand("deactivate", "Stop a queue type from taking tickets", "Deactivated", false)
	cmd.Long = `Stop a queue type from taking tickets without deleting it.

Searching tickets without any other active queue type are deleted, running matches keep going.`
	return cmd
}

func newSetActiveCommand(use, short, done string, active bool) *cobra.Command {
	return &cobra.Command{
		Use:               use + " <name>",
		Short:             short,
		Args:              cobra.ExactArgs(1),
		ValidArgsFunction: completeNames,
		RunE: func(cmd *cobra.Command, args []string) error {
			c, err := client.Connect(cmd)
			if err != nil {
				return err
			}
			defer c.Close()

			req := queuev1.SetQueueTypeActiveRequest_builder{Name: args[0], Active: active}.Build()
			if _, err := c.Types.SetQueueTypeActive(cmd.Context(), req); err != nil {
				return err
			}
			fmt.Fprintf(cmd.OutOrStdout(), "%s queue type %s\n", done, args[0])
			return nil
		},
	}
}
