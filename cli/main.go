package main

import (
	"os"

	"github.com/mythicisland/queue/cli/cmd"
	"github.com/mythicisland/queue/cli/internal/output"
)

func main() {
	if err := cmd.Execute(); err != nil {
		output.Error(os.Stderr, err)
		os.Exit(1)
	}
}
