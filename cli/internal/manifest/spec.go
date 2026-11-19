package manifest

import (
	"errors"
	"strconv"
	"strings"
	"time"

	queuev1 "buf.build/gen/go/mythicisland/queue/protocolbuffers/go/mythicisland/queue/v1"
	"google.golang.org/protobuf/proto"
	"google.golang.org/protobuf/types/known/durationpb"
)

var (
	errPlayers = errors.New("players must look like 2-16 or 8")
	errTeams   = errors.New("teams must look like 2x4")
)

type Spec struct {
	Name       string
	Group      string
	Shared     bool
	MinPlayers uint32
	MaxPlayers uint32
	TeamCount  uint32
	TeamSize   uint32
	MaxWait    time.Duration
}

func (s Spec) QueueType() *queuev1.QueueType {
	server := queuev1.ServerConfig_builder{Group: s.Group}
	if s.Shared {
		server.Shared = queuev1.SharedMode_builder{}.Build()
	} else {
		server.Exclusive = queuev1.ExclusiveMode_builder{}.Build()
	}

	match := queuev1.MatchConfig_builder{
		MinPlayers: s.MinPlayers,
		MaxPlayers: s.MaxPlayers,
	}
	if s.TeamCount > 0 {
		match.Teams = queuev1.TeamConfig_builder{Count: s.TeamCount, Size: s.TeamSize}.Build()
		match.MinPlayers = s.TeamCount * s.TeamSize
		match.MaxPlayers = 0
	}
	if s.MaxWait > 0 {
		match.MaxWait = durationpb.New(s.MaxWait)
	}

	return queuev1.QueueType_builder{
		Name:   s.Name,
		Server: server.Build(),
		Match:  match.Build(),
		Active: proto.Bool(true),
	}.Build()
}

func ParsePlayers(value string) (uint32, uint32, error) {
	minValue, maxValue, isRange := strings.Cut(value, "-")
	if !isRange {
		maxValue = minValue
	}

	minPlayers, err := parseCount(minValue)
	if err != nil {
		return 0, 0, errPlayers
	}
	maxPlayers, err := parseCount(maxValue)
	if err != nil {
		return 0, 0, errPlayers
	}
	return minPlayers, maxPlayers, nil
}

func ParseTeams(value string) (uint32, uint32, error) {
	countValue, sizeValue, ok := strings.Cut(value, "x")
	if !ok {
		return 0, 0, errTeams
	}

	count, err := parseCount(countValue)
	if err != nil {
		return 0, 0, errTeams
	}
	size, err := parseCount(sizeValue)
	if err != nil {
		return 0, 0, errTeams
	}
	return count, size, nil
}

func parseCount(value string) (uint32, error) {
	count, err := strconv.ParseUint(strings.TrimSpace(value), 10, 32)
	return uint32(count), err
}
