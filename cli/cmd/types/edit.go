package types

import (
	"fmt"

	queuev1 "buf.build/gen/go/mythicisland/queue/protocolbuffers/go/mythicisland/queue/v1"
	"github.com/mythicisland/queue/cli/internal/client"
	"github.com/mythicisland/queue/cli/internal/editor"
	"github.com/mythicisland/queue/cli/internal/manifest"
	"github.com/mythicisland/queue/cli/internal/output"
	"github.com/spf13/cobra"
)

const reference = `
# All options, copy what you need into the config above:
#
# server:
#   group: bedwars
#   allocation_timeout: 30s      # how long a match waits for a server
#   exclusive: {}                # one match per server
#   shared:                      # or several matches per server
#     capacity: 4
#     accept_timeout: 10s
# match:
#   min_players: 2
#   max_players: 16
#   teams:                       # replaces min_players and max_players
#     count: 4
#     size: 4
#     balance_by: elo            # ticket property to balance teams by
#   parties:
#     max_size: 4
#   fill_time: 15s               # wait for more players once min_players is reached
#   countdown: 10s
#   max_wait: 5m                 # tickets are removed after searching this long
#   max_failures: 3
#   rules:                       # tickets only match when their properties fit
#     - key: region
#       equal:
#         relax_after: 30s
#     - key: elo
#       range: { start: 100, widen: 50, every: 10s, max: 500 }
#   filters:                     # tickets without a fitting property can't join
#     - key: version
#       values: { values: ["1.21"] }
#     - key: elo
#       number: { min: 0, max: 3000 }
#     - key: premium
#       presence: { present: true }
#   quality:
#     spread:
#       - key: elo
#         window: { start: 100, widen: 50, every: 10s, max: 500 }
#     team_balance: { start: 50, widen: 25, every: 10s }
# properties:
#   mode: ranked
# active: true
`

func newEditCommand() *cobra.Command {
	return &cobra.Command{
		Use:   "edit <name>",
		Short: "Edit a queue type",
		Long: `Opens the queue type as YAML in the built-in editor and updates it on save.
Set $QUEUE_EDITOR to use your own editor instead, e.g. QUEUE_EDITOR="code --wait".

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

			save := func(edited []byte) error {
				queueType, err := manifest.Decode(edited)
				if err != nil {
					return err
				}
				if queueType.GetName() != name {
					return fmt.Errorf("queue types can't be renamed, create a new one and delete %s", name)
				}
				req := queuev1.UpdateQueueTypeRequest_builder{QueueType: queueType}.Build()
				_, err = c.Types.UpdateQueueType(cmd.Context(), req)
				return err
			}

			content := append(original, reference...)
			saved, err := editor.Edit("Queue type "+name, "queue-"+name+"-*.yaml", content, save)
			if err != nil {
				return err
			}
			if !saved {
				output.Info(cmd.OutOrStdout(), "No changes")
				return nil
			}
			output.Success(cmd.OutOrStdout(), "Updated queue type %s", name)
			return nil
		},
	}
}
