package client

import (
	"errors"
	"fmt"
	"strings"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

func readable(err error, url string) error {
	if err == nil {
		return nil
	}
	st, ok := status.FromError(err)
	if !ok {
		return err
	}

	switch st.Code() {
	case codes.Unavailable:
		return fmt.Errorf("queue at %s is not available: %s", url, st.Message())
	case codes.DeadlineExceeded:
		return fmt.Errorf("queue at %s did not answer in time", url)
	case codes.Unauthenticated:
		return fmt.Errorf("queue at %s rejected the secret", url)
	case codes.InvalidArgument:
		problems := strings.Split(st.Message(), "; ")
		if len(problems) == 1 {
			return errors.New(problems[0])
		}
		return errors.New("invalid queue type:\n  - " + strings.Join(problems, "\n  - "))
	default:
		return errors.New(st.Message())
	}
}
