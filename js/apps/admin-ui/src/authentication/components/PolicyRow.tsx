import {
    Button,
    DataListCell,
    DataListControl,
    DataListDragButton,
    DataListItem,
    DataListItemCells,
    DataListItemRow, DataListToggle,
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
import {AddFlowDropdown} from "./AddFlowDropdown";
import {EditFlow} from "./EditFlow";
import {AddPolicyFlowDropdown} from "./AddPolicyFlowDropdown";
import type {
    AuthenticationProviderRepresentation
} from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import {FlowRow} from "./FlowRow";

type PolicyRowProps = {
    builtIn: boolean;
    execution: ExpandableExecution;
    isParentPolicy: boolean;
    onRowClick: (execution: ExpandableExecution) => void;
    onRowChange: (execution: ExpandableExecution) => void;
    onAddExecution: (
        execution: ExpandableExecution,
        type: AuthenticationProviderRepresentation,
    ) => void;
    onDelete: (execution: ExpandableExecution) => void;
};

export const PolicyRow = ({
                            builtIn,
                            execution,
                            isParentPolicy,
                            onRowClick,
                            onRowChange,
                            onAddExecution,
                            onDelete,
                        }: PolicyRowProps) => {
    const { t } = useTranslation();
    const {realm} = useRealm();
    const navigate = useNavigate();
    const hasSubList = !!execution.executionList?.length;

    return (
        <>
            <Draggable key={`draggable-${execution.id}`} hasNoWrapper>
                <DataListItem
                    className="keycloak__authentication__flow-item"
                    id={execution.id}
                    isExpanded={!isParentPolicy && !execution.isCollapsed}
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
                        {!isParentPolicy && hasSubList && (
                            <DataListToggle
                                onClick={() => onRowClick(execution)}
                                isExpanded={!execution.isCollapsed}
                                id={`toggle1-${execution.id}`}
                                aria-controls={execution.executionList![0].id}
                            />
                        )}
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
                                    {isParentPolicy && (
                                        <Button
                                            data-testid="policyDetail"
                                            variant="secondary"
                                            onClick={() => navigate(toAuthenticationPolicy({
                                                realm,
                                                id: execution.id!
                                            }))}
                                        >
                                            {t("details")}
                                        </Button>
                                    )}
                                </DataListCell>,
                                <DataListCell key={`${execution.id}-config`}>
                                    {!isParentPolicy && (
                                        <>
                                            <AddPolicyFlowDropdown
                                                execution={execution}
                                                onAddExecution={onAddExecution}
                                            />
                                            <EditFlow
                                                execution={execution}
                                                onRowChange={onRowChange}
                                            />
                                        </>
                                    )}
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
            {!isParentPolicy &&
                !execution.isCollapsed &&
                hasSubList &&
                execution.executionList?.map((ex) => (
                    <FlowRow
                        builtIn={builtIn}
                        key={ex.id}
                        execution={ex}
                        onRowClick={onRowClick}
                        onRowChange={onRowChange}
                        onAddExecution={onAddExecution}
                        onAddFlow={() => null}
                        onDelete={onDelete}
                    />
                ))}
        </>
    );
};
