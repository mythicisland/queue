package types

import (
	queuev1 "buf.build/gen/go/mythicisland/queue/protocolbuffers/go/mythicisland/queue/v1"
	"github.com/mythicisland/queue/cli/internal/client"
	"github.com/mythicisland/queue/cli/internal/output"
	"github.com/spf13/cobra"
)

func newGetCommand() *cobra.Command {
	return &cobra.Command{
		Use:               "get <name>",
		Short:             "Show the config of a queue type",
		Args:              cobra.ExactArgs(1),
		ValidArgsFunction: completeNames,
		RunE: func(cmd *cobra.Command, args []string) error {
			format, err := output.Format(cmd, output.FormatYAML)
			if err != nil {
				return err
			}

			c, err := client.Connect(cmd)
			if err != nil {
				return err
			}
			defer c.Close()

			req := queuev1.GetQueueTypeRequest_builder{Name: args[0]}.Build()
			res, err := c.Data.GetQueueType(cmd.Context(), req)
			if err != nil {
				return err
			}

			if format == output.FormatTable {
				return printTable(cmd, c, []*queuev1.QueueType{res.GetQueueType()})
			}
			return output.Print(cmd.OutOrStdout(), res.GetQueueType(), format)
		},
	}
}
