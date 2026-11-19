package plan

import (
	"context"
	"errors"
	"fmt"
	"slices"
	"strings"

	queuev1 "buf.build/gen/go/mythicisland/queue/protocolbuffers/go/mythicisland/queue/v1"
	"github.com/mythicisland/queue/cli/internal/client"
	"google.golang.org/protobuf/proto"
)

type Action string

const (
	Create    Action = "create"
	Update    Action = "update"
	Delete    Action = "delete"
	Unchanged Action = "unchanged"
)

type Change struct {
	Action  Action
	Current *queuev1.QueueType
	Wanted  *queuev1.QueueType
}

func (c Change) Name() string {
	if c.Wanted != nil {
		return c.Wanted.GetName()
	}
	return c.Current.GetName()
}

// Make compares with what the queue would store, not with the files, because the queue fills in defaults.
func Make(ctx context.Context, c *client.Client, wanted []*queuev1.QueueType, prune bool) ([]Change, error) {
	res, err := c.Data.ListQueueTypes(ctx, &queuev1.ListQueueTypesRequest{})
	if err != nil {
		return nil, err
	}
	current := map[string]*queuev1.QueueType{}
	for _, queueType := range res.GetQueueTypes() {
		current[queueType.GetName()] = queueType
	}

	var changes []Change
	var problems []error
	for _, queueType := range wanted {
		existing := current[queueType.GetName()]
		delete(current, queueType.GetName())
		checked, err := check(ctx, c, queueType, existing != nil)
		if err != nil {
			problems = append(problems, fmt.Errorf("%s: %w", queueType.GetName(), err))
			continue
		}

		switch {
		case existing == nil:
			changes = append(changes, Change{Action: Create, Wanted: checked})
		case proto.Equal(existing, checked):
			changes = append(changes, Change{Action: Unchanged, Current: existing, Wanted: checked})
		default:
			changes = append(changes, Change{Action: Update, Current: existing, Wanted: checked})
		}
	}

	if prune {
		for _, queueType := range current {
			changes = append(changes, Change{Action: Delete, Current: queueType})
		}
	}

	slices.SortFunc(changes, func(a, b Change) int {
		return strings.Compare(a.Name(), b.Name())
	})
	return changes, errors.Join(problems...)
}

func check(ctx context.Context, c *client.Client, queueType *queuev1.QueueType, exists bool) (*queuev1.QueueType, error) {
	if exists {
		req := queuev1.UpdateQueueTypeRequest_builder{QueueType: queueType, ValidateOnly: true}.Build()
		res, err := c.Types.UpdateQueueType(ctx, req)
		return res.GetQueueType(), err
	}
	req := queuev1.CreateQueueTypeRequest_builder{QueueType: queueType, ValidateOnly: true}.Build()
	res, err := c.Types.CreateQueueType(ctx, req)
	return res.GetQueueType(), err
}

func Apply(ctx context.Context, c *client.Client, change Change) error {
	var err error
	switch change.Action {
	case Create:
		_, err = c.Types.CreateQueueType(ctx, queuev1.CreateQueueTypeRequest_builder{QueueType: change.Wanted}.Build())
	case Update:
		_, err = c.Types.UpdateQueueType(ctx, queuev1.UpdateQueueTypeRequest_builder{QueueType: change.Wanted}.Build())
	case Delete:
		_, err = c.Types.DeleteQueueType(ctx, queuev1.DeleteQueueTypeRequest_builder{Name: change.Name()}.Build())
	}
	if err != nil {
		return fmt.Errorf("%s: %w", change.Name(), err)
	}
	return nil
}
