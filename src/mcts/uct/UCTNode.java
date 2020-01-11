package mcts.uct;

import framework.IBoard;
import framework.MoveList;
import framework.Options;
import framework.util.FastLog;
import mcts.State;
import mcts.TransposTable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class UCTNode {
    private final Options options;
    private final TransposTable tt;
    private final long hash;
    public final int player;
    public final int[] move;
    private boolean expanded = false, simulated = false;
    public List<UCTNode> children;
    private double[] RAVEvalue = {0, 0};
    private int RAVEVisits = 0;
    // For debug only
    public String boardString;
    public ArrayList<Double> timeSeries;

    public State state;

    /**
     * Constructor for the root
     */
    public UCTNode(int player, Options options, long hash, TransposTable tt) {
        this.player = player;
        this.options = options;
        this.tt = tt;
        this.hash = hash;
        this.state = tt.getState(hash, true);
        this.move = null;
    }

    /**
     * Constructor for internal node
     */
    public UCTNode(int player, int[] move, Options options, long hash, TransposTable tt) {
        this.player = player;
        this.move = move;
        this.options = options;
        this.tt = tt;
        this.hash = hash;
        this.state = tt.getState(hash, true);
        if (options.debug)
            timeSeries = new ArrayList<>();
    }

    public double[] MCTS(IBoard board, int depth) {
        assert board.getPlayerToMove() == player : "Incorrect player to move";
        // TODO Build in some more assertions here
        UCTNode child = null;
        // First add some leafs if required
        if (!expanded)
            child = expand(board); // Expand returns any node that leads to a win

        // Select the best child, if we didn't find a winning position in the expansion
        if (child == null)
            if (isTerminal())
                child = this;
            else
                child = select();

        double[] result = {-1000, -1000};
        // (Solver) Check for proven win / loss / draw
        if (!child.isSolved()) {
            // Execute the move represented by the child
            if (!isTerminal()) {
                if (options.RAVE)
                    options.addRAVEMove(player, board.getMoveId(child.move), depth);

                if (options.MAST)
                    options.addMASTMove(player, board.getMoveId(child.move));

                board.doMove(child.move);
                assert board.hash() == child.hash : "Board hash is incorrect";
            }
            // When a leaf is reached return the result of the playout
            if (!child.simulated || isTerminal()) {
                result = child.playOut(board, depth + 1); // WARN a single copy of the board is used
                child.updateStats(result);
                child.simulated = true;
            } else {
                result = child.MCTS(board, depth + 1);
                // Update the RAVE value if this move was played
                if (options.RAVE && !child.isSolved()) {
                    for (UCTNode c : children) {
                        // No need to update the selected child, only siblings
                        if (c.equals(child) || c.isSolved())
                            continue;

                        if (options.isRAVEMove(player, board.getMoveId(c.move), depth)) {
                            c.updateRAVE(result);
                        }
                    }
                }
            }
            //
            assert result[0] != -1000 && result[1] != -1000 : "Strange result";
            if (!child.isSolved())
                updateStats(result);
            // For displaying the time-series charts
            if (Options.debug && depth == 0)
                child.timeSeries.add(child.getValue(player));
        }

        if (child.getValue(player) == Integer.MAX_VALUE) {
            // One of my children is a proven win
            setSolved(player);
            // Backprop a win
            result = new double[2];
            result[player - 1] = 1;
            result[(3 - player) - 1] = -1;
            return result;
        } else if (child.getValue(player) == Integer.MIN_VALUE) {
            result = new double[2];
            result[player - 1] = -1;
            result[(3 - player) - 1] = 1;
            // Check if all children are a proven loss
            for (UCTNode c : children) {
                if (c.getValue(player) != Integer.MIN_VALUE) {
                    // Return a single loss, if not all children are a loss
                    updateStats(result);
                    return result;
                }
            }
            setSolved(3 - player); // I'm a proven win for the parent
            return result;
        }

        assert (result[0] > Integer.MIN_VALUE) && (result[1] > Integer.MIN_VALUE) : "Result not initialized";

        return result;
    }

    private UCTNode expand(IBoard board) {
        // If one of the nodes is a win, we don't have to select
        UCTNode winNode = null;
        MoveList moves = board.getExpandMoves();
        if (children == null)
            children = new LinkedList<>();

        int winner = board.checkWin();
        assert winner == IBoard.NONE_WIN || winner == IBoard.DRAW : "Trying to expand a proven node";
        // Board is terminal, don't expand
        if (winner != IBoard.NONE_WIN)
            return null;

        int[] move;
        double[] best_imVal = {Integer.MIN_VALUE, Integer.MIN_VALUE};
        // Add all moves as children to the current node
        for (int i = 0; i < moves.size(); i++) {
            move = moves.get(i);

            IBoard tempBoard = board.clone();
            tempBoard.doMove(move);

            UCTNode child = new UCTNode(3 - player, move, options, tempBoard.hash(), tt);

            if (Options.debug)
                child.boardString = tempBoard.toString();

            // We've expanded an already proven won node
            if (child.isSolved() && child.getValue(player) == Integer.MAX_VALUE)
                winNode = child;
            else {
                // Check for a winner, (Solver)
                winner = tempBoard.checkWin();
                if (winner == IBoard.P1 || winner == IBoard.P2) {
                    if (winner == player)
                        winNode = child;
                    child.setSolved(winner);
                }
            }

            // implicit minimax
            if (options.imm) {
                double[] imVal = new double[2];
                if (child.getImValue()[0] == Integer.MIN_VALUE) {
                    imVal[player - 1] = tempBoard.evaluate(player);
                    imVal[(3 - player) - 1] = -imVal[player - 1];
                    child.setImValue(imVal);
                } else // IM Value was already determined elsewhere in the tree
                    imVal = child.getImValue();

                if (imVal[player - 1] > best_imVal[player - 1])
                    best_imVal = imVal;
            }
            children.add(child);
        }
        expanded = true;

        // Back-propagate the best IM value
        if (options.imm)
            setImValue(best_imVal);

        // If one of the nodes is a win, return it.
        return winNode;
    }

    private UCTNode select() {
        double maxIm = Integer.MIN_VALUE, minIm = Integer.MIN_VALUE;
        if (options.imm) {
            double val;
            // Check the highest and lowest evaluation for normalization
            for (UCTNode c : children) {
                val = c.getImValue()[player - 1];
                if (val > maxIm)
                    maxIm = val;
                if (val < minIm)
                    minIm = val;
            }
        }

        UCTNode selected = null;
        double max = Double.NEGATIVE_INFINITY;
        double uctValue, np = getVisits();
        // Select a child according to the UCT Selection policy
        for (UCTNode c : children) {
            double nc = c.getVisits(), val = c.getValue(player);
            // Always select a proven win
            if (val == Integer.MAX_VALUE)
                uctValue = Integer.MAX_VALUE - Options.r.nextDouble();
            else if (val == Integer.MIN_VALUE)
                uctValue = Integer.MIN_VALUE + Options.r.nextDouble();
            else if (nc == 0)
                // First, visit all children at least once
                uctValue = 100. + Options.r.nextDouble();
            else {
                // Linear regression
                if (options.regression && c.state != null) { // TODO Put this logic in a getter
                    double regVal = c.state.getRegressionValue(options.regForecastSteps, player);
                    if (regVal > Integer.MIN_VALUE) // This value is returned if there weren't enough visits to predict
                        val = (1. - options.regAlpha) * val + (options.regAlpha * regVal);
                }

                // Implicit minimax
                if (options.imm && minIm != maxIm) {
                    double imVal = (c.getImValue()[player - 1] - minIm) / (maxIm - minIm);
                    val = (1. - options.imAlpha) * val + (options.imAlpha * imVal);
                }

                if (options.RAVE && c.RAVEVisits > 0) {
                    double beta = Math.sqrt(options.k / ((3 * c.getVisits()) + options.k));
                    val = (beta * c.getRAVE(player)) + ((1 - beta) * val);
                }

                // Compute the uct value with the (new) average value
                uctValue = val + options.c * Math.sqrt(FastLog.log(np) / nc) + (Options.r.nextDouble() * 0.001);
            }

            // Remember the highest UCT value
            if (uctValue > max) {
                selected = c;
                max = uctValue;
            }
        }
        return selected;
    }

    private double[] playOut(IBoard board, int depth) {
        int winner = board.checkWin(), nMoves = 0, moveIndex;
        assert winner == IBoard.NONE_WIN || winner == IBoard.DRAW : "Board in won position in playout.";

        int[] move;
        boolean interrupted = false;
        MoveList moves;
        double mastMax, mastVal;
        do {
            moves = board.getPlayoutMoves(options.heuristics);
            // No more moves to be made
            if (moves.size() == 0)
                break;
            moveIndex = Options.r.nextInt(moves.size());
            if (options.MAST && Options.r.nextDouble() < (1. - options.epsilon)) {
                mastMax = Double.NEGATIVE_INFINITY;
                // Select the move with the highest MAST value
                for (int i = 0; i < moves.size(); i++) {
                    mastVal = options.getMASTValue(board.getPlayerToMove(), board.getMoveId(moves.get(i)));
                    // If bigger, we have a winner, if equal, flip a coin
                    if (mastVal > mastMax || (mastVal == mastMax && Options.r.nextDouble() < .5)) {
                        mastMax = mastVal;
                        moveIndex = i;
                    }
                }
            }
            move = moves.get(moveIndex);
            ++nMoves;
            if (options.RAVE) // Insert the rave and mast move before the play is made to get the correct player to move
                options.addRAVEMove(board.getPlayerToMove(), board.getMoveId(move), depth + nMoves);
            if (options.MAST)
                options.addMASTMove(board.getPlayerToMove(), board.getMoveId(move));
            board.doMove(move);
            winner = board.checkWin();
            // Interrupt playout for early evaluation
            if (winner == IBoard.NONE_WIN && options.earlyTerm && nMoves == options.termDepth)
                interrupted = true;

        } while (winner == IBoard.NONE_WIN && !interrupted);

        double[] score = {0, 0};

        if (options.earlyTerm) {
            if (!interrupted) {
                score[winner - 1] += options.etWv;
                score[(3 - winner) - 1] -= options.etWv;
            } else {
                double eval = board.evaluate(1); // TODO - Is it better to include the value of the evaluation here?
                if (eval > options.etT) {
                    score[0] = 1;
                    score[1] = -1;
                } else if (eval < -options.etT) {
                    score[1] = 1;
                    score[0] = -1;
                }
            }
        } else if (winner != IBoard.DRAW) {
            score[winner - 1] = 1;
            score[(3 - winner) - 1] = -1;
        } else {
            score[0] = 0;
            score[1] = 0;
        }

        if (options.MAST)
            options.updateMASTMoves(score);

        return score;
    }

    public UCTNode getBestChild() {
        if (children == null)
            return null;
        double max = Double.NEGATIVE_INFINITY, value;
        UCTNode bestChild = null;
        for (UCTNode c : children) {
            // If there are children with INF value, choose one of them
            if (c.getValue(player) == Integer.MAX_VALUE)
                value = Integer.MAX_VALUE - Options.r.nextDouble();
            else if (c.getValue(player) == Integer.MIN_VALUE)
                value = Integer.MIN_VALUE + c.getVisits() + Options.r.nextDouble();
            else {
                value = c.getVisits();
            }

            if (value > max) {
                max = value;
                bestChild = c;
            }
        }
        return bestChild;
    }

    private void updateStats(double[] value) {
        assert value[0] > Integer.MIN_VALUE && value[1] > Integer.MIN_VALUE : "Wrong values in updateStats";

        if (state == null)
            state = tt.getState(hash, false);
        //
        state.updateStats(value, options.regression);

        // implicit minimax backups
        if (options.imm && children != null) {
            double[] bestVal = {Integer.MIN_VALUE, Integer.MIN_VALUE};
            // TODO Check if this should minimize or maximize
            for (UCTNode c : children) {
                if (c.getImValue()[player - 1] > bestVal[player - 1]) {
                    bestVal = c.getImValue();
                }
            }
            setImValue(bestVal);
        }
    }

    private void setSolved(int player) {
        if (state == null)
            state = tt.getState(hash, false);

        state.setSolved(player);
    }

    public boolean isSolved() {
        if (state == null)
            state = tt.getState(hash, true);

        if (state == null)
            return false;

        return state.isSolved();
    }

    private void setImValue(double[] imValue) {
        if (state == null)
            state = tt.getState(hash, false);

        state.setImValue(imValue);
    }

    private double[] getImValue() {
        if (state == null)
            state = tt.getState(hash, true);

        if (state == null)
            return new double[]{Integer.MIN_VALUE, Integer.MIN_VALUE};

        return state.getImValue();
    }

    /**
     * @return The value of this node with respect to the parent
     */
    public double getValue(int player) {
        if (state == null)
            state = tt.getState(hash, true);

        if (state == null)
            return 0;

        return state.getMean(player);
    }

    public void updateRAVE(double[] values) {
        RAVEvalue[0] += values[0];
        RAVEvalue[1] += values[1];
        RAVEVisits++;
    }

    public double getRAVE(int player) {
        if (RAVEVisits > 0)
            return RAVEvalue[player - 1] / RAVEVisits;
        else
            return 0;
    }

    private double getVisits() {
        if (state == null)
            state = tt.getState(hash, true);

        if (state == null)
            return 0.;

        return state.getVisits();
    }

    public boolean isTerminal() {
        return children != null && children.size() == 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(move[0]).append(".").append(move[1]).append(" ");

        if (state != null)
            sb.append(state.toString());

        if (RAVEVisits > 0) {
            sb.append(" :: RAVE 1: ").append(State.df2.format(getRAVE(1))).append(" RAVE 2: ").
                    append(State.df2.format(getRAVE(2))).append(" Rn: ").append(RAVEVisits);
        }
        return sb.toString();
    }

    public String toString(IBoard board) {

        StringBuilder sb = new StringBuilder();
        sb.append(board.getMoveString(move));
        if (state != null)
            sb.append("\t :: ").append(state.toString());
        else
            sb.append(" :: ").append(" no state");
        if (RAVEVisits > 0) {
            sb.append("\t :: RAVE 1: ").append(State.df2.format(getRAVE(1))).append(" RAVE 2: ").
                    append(State.df2.format(getRAVE(2))).append(" Rn: ").append(RAVEVisits);
        }

        return sb.toString();
    }
}
