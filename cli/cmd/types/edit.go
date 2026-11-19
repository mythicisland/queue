package types

import (
	"bytes"
	"fmt"
	"os"

	queuev1 "buf.build/gen/go/mythicisland/queue/protocolbuffers/go/mythicisland/queue/v1"
	"github.com/mythicisland/queue/cli/internal/client"
	"github.com/mythicisland/queue/cli/internal/editor"
	"github.com/mythicisland/queue/cli/internal/manifest"
	"github.com/spf13/cobra"
)

func newEditCommand() *cobra.Command {
	return &cobra.Command{
		Use:   "edit <name>",
		Short: "Edit a queue type in your editor",
		Long: `Opens the queue type as YAML in $QUEUE_EDITOR, $VISUAL or $EDITOR and updates it when the editor closes.

Running matches keep going with the old config.`,
		Args:              cobra.ExactArgs(1),
		ValidArgsFunction: completeNames,
		RunE: func(cmd *cobra.Command, args []string) error {
			name := args[0]
			c, err := client.Connect(cmd)
			if err != nil {
				return err
			}
			defer c.Close()

			res, err := c.Data.GetQueueType(cmd.Context(), queuev1.GetQueueTypeRequest_builder{Name: name}.Build())
			if err != nil {
				return err
			}
			original, err := manifest.Encode(res.GetQueueType(), manifest.YAML)
			if err != nil {
				return err
			}

			edited, path, err := editor.Edit("queue-"+name+"-*.yaml", original)
			if err != nil {
				return err
			}
			if bytes.Equal(edited, original) {
				os.Remove(path)
				fmt.Fprintln(cmd.OutOrStdout(), "No changes")
				return nil
			}

			queueType, err := manifest.Decode(edited)
			if err != nil {
				return keptAt(err, path)
			}
			if queueType.GetName() != name {
				return keptAt(fmt.Errorf("queue types can't be renamed, create a new one and delete %s", name), path)
			}

			req := queuev1.UpdateQueueTypeRequest_builder{QueueType: queueType}.Build()
			if _, err := c.Types.UpdateQueueType(cmd.Context(), req); err != nil {
				return keptAt(err, path)
			}

			os.Remove(path)
			fmt.Fprintf(cmd.OutOrStdout(), "Updated queue type %s\n", name)
			return nil
		},
	}
}

func keptAt(err error, path string) error {
	return fmt.Errorf("%w\nyour changes are saved in %s", err, path)
}
