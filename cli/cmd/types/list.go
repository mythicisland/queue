package types

import (
	"slices"
	"strings"

	queuev1 "buf.build/gen/go/mythicisland/queue/protocolbuffers/go/mythicisland/queue/v1"
	"github.com/mythicisland/queue/cli/internal/client"
	"github.com/mythicisland/queue/cli/internal/manifest"
	"github.com/mythicisland/queue/cli/internal/output"
	"github.com/spf13/cobra"
)

func newListCommand() *cobra.Command {
	return &cobra.Command{
		Use:     "list",
		Aliases: []string{"ls"},
		Short:   "List all queue types",
		Args:    cobra.NoArgs,
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

			res, err := c.Data.ListQueueTypes(cmd.Context(), &queuev1.ListQueueTypesRequest{})
			if err != nil {
				return err
			}
			queueTypes := res.GetQueueTypes()
			slices.SortFunc(queueTypes, func(a, b *queuev1.QueueType) int {
				return strings.Compare(a.GetName(), b.GetName())
			})

			if format != output.FormatTable {
				data, err := manifest.EncodeList(queueTypes, format)
				if err != nil {
					return err
				}
				_, err = cmd.OutOrStdout().Write(data)
				return err
			}

			w := cmd.OutOrStdout()
			if len(queueTypes) == 0 {
				output.Info(w, "No queue types yet")
				output.Next(w, output.Step{Command: "queue types create", Description: "Create one step by step"})
				return nil
			}
			if err := printTable(cmd, c, queueTypes); err != nil {
				return err
			}
			output.Next(w,
				output.Step{Command: "queue types get <name>", Description: "Show the full config"},
				output.Step{Command: "queue types edit <name>", Description: "Change it in your editor"},
			)
			return nil
		},
	}
}
