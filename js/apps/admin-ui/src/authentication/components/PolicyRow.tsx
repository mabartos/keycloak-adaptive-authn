import {
    Button,
    DataListCell,
    DataListControl,
    DataListDragButton,
    DataListItem,
    DataListItemCells,
    DataListItemRow,
    Draggable,
    Text,
    TextVariants,
    Tooltip,
} from "@patternfly/react-core";
import {TrashIcon} from "@patternfly/react-icons";
import {useTranslation} from "react-i18next";
import type {ExpandableExecution} from "../execution-model";
import {FlowRequirementDropdown} from "./FlowRequirementDropdown";
import type {Flow} from "./modals/AddSubFlowModal";

import "./flow-row.css";
import {useNavigate} from "react-router-dom";
import {useRealm} from "../../context/realm-context/RealmContext";
import {toFlow} from "../routes/Flow";
import {toAuthenticationPolicy} from "../routes/AuthenticationPolicy";

type PolicyRowProps = {
    builtIn: boolean;
    execution: ExpandableExecution;
    onRowClick: (execution: ExpandableExecution) => void;
    onRowChange: (execution: ExpandableExecution) => void;
    onAddFlow: (execution: ExpandableExecution, flow: Flow) => void;
    onDelete: (execution: ExpandableExecution) => void;
};

export const PolicyRow = ({
                            builtIn,
                            execution,
                            onRowClick,
                            onRowChange,
                            onAddFlow,
                            onDelete,
                        }: PolicyRowProps) => {
    const { t } = useTranslation();
    const {realm} = useRealm();
    const navigate = useNavigate();
    return (
        <>
            <Draggable key={`draggable-${execution.id}`} hasNoWrapper>
                <DataListItem
                    className="keycloak__authentication__flow-item"
                    id={execution.id}
                    isExpanded={false}
                    aria-labelledby={`title-id-${execution.id}`}
                >
                    <DataListItemRow
                        className="keycloak__authentication__flow-row"
                        aria-level={execution.level! + 1}
                        role="heading"
                        aria-labelledby={execution.id}
                    >
                        <DataListControl>
                            <DataListDragButton aria-label={t("dragHelp")} />
                        </DataListControl>
                        <DataListItemCells
                            dataListCells={[
                                <DataListCell key={`${execution.id}-name`}>
                                        <>
                                            {execution.displayName} <br />{" "}
                                            <Text component={TextVariants.small}>
                                                {execution.alias} {execution.description}
                                            </Text>
                                        </>
                                </DataListCell>,
                                <DataListCell key={`${execution.id}-requirement`}>
                                    <FlowRequirementDropdown
                                        flow={execution}
                                        onChange={onRowChange}
                                    />
                                </DataListCell>,
                                <DataListCell key={`${execution.id}-detail`}>
                                    <Button
                                        data-testid="policyDetail"
                                        variant="secondary"
                                        /*TODO change*/
                                        /*onClick={() => navigate(toFlow({
                                            realm,
                                            id: execution.id!,
                                            usedBy: "notInUse",
                                            builtIn: undefined
                                        }))}*/
                                        onClick={() => navigate(toAuthenticationPolicy({
                                            realm,
                                            id: execution.id!
                                        }))}
                                    >
                                        {t("details")}
                                    </Button>
                                </DataListCell>,
                                <DataListCell key={`${execution.id}-config`}>
                                    {!builtIn && (
                                        <Tooltip content={t("delete")}>
                                            <Button
                                                variant="plain"
                                                data-testid={`${execution.displayName}-delete`}
                                                aria-label={t("delete")}
                                                onClick={() => onDelete(execution)}
                                            >
                                                <TrashIcon />
                                            </Button>
                                        </Tooltip>
                                    )}
                                </DataListCell>,
                            ]}
                        />
                    </DataListItemRow>
                </DataListItem>
            </Draggable>
        </>
    );
};
