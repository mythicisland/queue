package types

import (
	"errors"
	"fmt"

	queuev1 "buf.build/gen/go/mythicisland/queue/protocolbuffers/go/mythicisland/queue/v1"
	"charm.land/huh/v2"
	"github.com/mythicisland/queue/cli/internal/client"
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
				confirmed := false
				err := huh.NewConfirm().
					Title(fmt.Sprintf("Delete queue type %s?", name)).
					Description("Tickets without any other queue type are deleted.").
					Affirmative("Delete").
					Negative("Cancel").
					Value(&confirmed).
					Run()
				if err != nil && !errors.Is(err, huh.ErrUserAborted) {
					return err
				}
				if !confirmed {
					return nil
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
			fmt.Fprintf(cmd.OutOrStdout(), "Deleted queue type %s\n", name)
			return nil
		},
	}
	cmd.Flags().BoolVarP(&yes, "yes", "y", false, "delete without asking")
	return cmd
}
